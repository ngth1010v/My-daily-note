package io.github.ngth1010v.mydailynote.speech.api

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

sealed interface ModelState {
    data object NotInstalled : ModelState
    /** [progress] is 0..1. */
    data class Downloading(val progress: Float) : ModelState
    data object Installing : ModelState
    data class Installed(val bytes: Long) : ModelState
    data class Failed(val reason: Reason) : ModelState

    enum class Reason { NETWORK, CHECKSUM, STORAGE, INVALID_FILE }
}

/**
 * Downloads, verifies (SHA-256), unpacks, imports and deletes speech models.
 * Models live in app-private storage: files/models/<id>/ (a Vosk folder or Whisper model.bin).
 * The only network use in the app is here.
 */
class ModelManager(context: Context, private val scope: CoroutineScope) {
    private val root = File(context.filesDir, "models")
    private val tmpRoot = File(context.cacheDir, "models")
    private val jobs = mutableMapOf<String, Job>()

    private val _states = MutableStateFlow(ModelRegistry.all.associate { it.id to scan(it) })
    val states: StateFlow<Map<String, ModelState>> = _states.asStateFlow()

    fun state(id: String): ModelState = _states.value[id] ?: ModelState.NotInstalled

    fun isInstalled(id: String): Boolean = state(id) is ModelState.Installed

    /** Folder (archive models) or file (single-file models) to load, or null if not installed. */
    fun path(id: String): File? {
        val spec = ModelRegistry.get(id) ?: return null
        if (!isInstalled(id)) return null
        return if (spec.archive) dir(id) else File(dir(id), MODEL_FILE)
    }

    fun download(id: String) {
        val spec = ModelRegistry.get(id) ?: return
        if (jobs[id]?.isActive == true) return
        jobs[id] = scope.launch(Dispatchers.IO) {
            set(id, ModelState.Downloading(0f))
            val tmp = File(tmpRoot, "$id.part").apply { parentFile?.mkdirs(); delete() }
            try {
                val hash = fetch(spec, tmp)
                if (!hash.equals(spec.sha256, ignoreCase = true)) {
                    set(id, ModelState.Failed(ModelState.Reason.CHECKSUM))
                    return@launch
                }
                set(id, ModelState.Installing)
                tmp.inputStream().use { install(spec, it) }
                set(id, scan(spec))
            } catch (e: IOException) {
                set(id, ModelState.Failed(if (e is StorageException) ModelState.Reason.STORAGE else ModelState.Reason.NETWORK))
            } finally {
                tmp.delete()
                if (state(id) is ModelState.Downloading) set(id, scan(spec)) // cancelled
            }
        }
    }

    fun cancel(id: String) {
        jobs.remove(id)?.cancel()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        cancel(id)
        dir(id).deleteRecursively()
        set(id, ModelState.NotInstalled)
    }

    /**
     * Installs a model from a user-picked file (zip for Vosk, .bin for Whisper), for devices that
     * should never go online. The checksum is not enforced (other model versions are allowed), but
     * the content must look like the right kind of model.
     */
    suspend fun import(id: String, input: InputStream): Boolean = withContext(Dispatchers.IO) {
        val spec = ModelRegistry.get(id) ?: return@withContext false
        set(id, ModelState.Installing)
        try {
            install(spec, input)
            set(id, scan(spec))
            isInstalled(id)
        } catch (e: IOException) {
            set(id, ModelState.Failed(ModelState.Reason.INVALID_FILE))
            false
        }
    }

    private fun set(id: String, state: ModelState) = _states.update { it + (id to state) }

    private fun dir(id: String) = File(root, id)

    private fun scan(spec: ModelSpec): ModelState {
        val d = dir(spec.id)
        if (!File(d, MARKER).exists()) return ModelState.NotInstalled
        return ModelState.Installed(d.walkTopDown().filter { it.isFile }.sumOf { it.length() })
    }

    /** Downloads to [dest], reporting progress; returns the SHA-256 hex. */
    private suspend fun fetch(spec: ModelSpec, dest: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var url = URL(spec.url)
        var conn: HttpURLConnection
        var redirects = 0
        while (true) {
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            val code = conn.responseCode
            if (code in 300..399 && redirects++ < 5) {
                url = URL(url, conn.getHeaderField("Location"))
                conn.disconnect()
                continue
            }
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            break
        }
        try {
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: spec.sizeBytes
            var done = 0L
            var lastReported = 0f
            conn.inputStream.use { input ->
                dest.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        try {
                            out.write(buf, 0, n)
                        } catch (e: IOException) {
                            throw StorageException(e)
                        }
                        digest.update(buf, 0, n)
                        done += n
                        val p = (done.toFloat() / total).coerceIn(0f, 1f)
                        if (p - lastReported >= 0.01f) {
                            lastReported = p
                            set(spec.id, ModelState.Downloading(p))
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Unpacks/copies into a staging folder, validates, then swaps it in. */
    private fun install(spec: ModelSpec, input: InputStream) {
        val staging = File(root, "${spec.id}.staging").apply { deleteRecursively(); mkdirs() }
        try {
            if (spec.archive) unzipStrippingTopDir(input, staging)
            else File(staging, MODEL_FILE).outputStream().use { input.copyTo(it) }
            if (!looksValid(spec, staging)) throw IOException("Invalid model content")
            File(staging, MARKER).writeText(spec.id)
            val target = dir(spec.id)
            target.deleteRecursively()
            if (!staging.renameTo(target)) throw StorageException(IOException("rename failed"))
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun looksValid(spec: ModelSpec, dir: File): Boolean = when (spec.kind) {
        ModelKind.VOSK -> File(dir, "am").isDirectory && File(dir, "conf").isDirectory
        ModelKind.WHISPER -> File(dir, MODEL_FILE).let { f ->
            // ggml model files start with the magic 0x67676d6c ("lmgg" little-endian).
            f.length() > 1_000_000 && f.inputStream().use { s ->
                val b = ByteArray(4)
                s.read(b) == 4 && String(b, Charsets.US_ASCII) == "lmgg"
            }
        }
    }

    private fun unzipStrippingTopDir(input: InputStream, dest: File) {
        val canonicalDest = dest.canonicalPath + File.separator
        ZipInputStream(input.buffered()).use { zip ->
            val entries = generateSequence { zip.nextEntry }
            for (entry in entries) {
                // Vosk zips wrap everything in one top folder; drop it.
                val name = entry.name.replace('\\', '/').substringAfter('/', "")
                if (name.isEmpty()) continue
                val out = File(dest, name)
                if (!out.canonicalPath.startsWith(canonicalDest)) throw IOException("Zip entry outside target")
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    try {
                        out.outputStream().use { zip.copyTo(it) }
                    } catch (e: IOException) {
                        throw StorageException(e)
                    }
                }
            }
        }
    }

    private class StorageException(cause: IOException) : IOException(cause)

    private companion object {
        const val MODEL_FILE = "model.bin"
        const val MARKER = ".installed"
    }
}
