package io.github.ngth1010v.mydailynote.speech.api

enum class ModelKind { VOSK, WHISPER }

/**
 * A downloadable speech model. [archive] models are zips unpacked into a folder (Vosk);
 * others are a single file (Whisper ggml). [sha256] is checked after every download.
 */
data class ModelSpec(
    val id: String,
    val kind: ModelKind,
    /** Null = multilingual. */
    val language: VoiceLanguage?,
    val displayName: String,
    val url: String,
    val sha256: String,
    val sizeBytes: Long,
    val archive: Boolean,
    val license: String,
)

object ModelRegistry {
    val all: List<ModelSpec> = listOf(
        ModelSpec(
            id = "vosk-small-en-us-0.15",
            kind = ModelKind.VOSK,
            language = VoiceLanguage.EN,
            displayName = "Vosk small English (US) 0.15",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
            sha256 = "30f26242c4eb449f948e42cb302dd7a686cb29a3423a8367f99ff41780942498",
            sizeBytes = 41_205_931,
            archive = true,
            license = "Apache-2.0",
        ),
        ModelSpec(
            id = "vosk-small-vn-0.4",
            kind = ModelKind.VOSK,
            language = VoiceLanguage.VI,
            displayName = "Vosk small Vietnamese 0.4",
            url = "https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip",
            sha256 = "efe5c8494212110471a79befc48c79da679e5b1fc52a4ffb500222ff86d622e5",
            sizeBytes = 33_656_337,
            archive = true,
            license = "Apache-2.0",
        ),
        ModelSpec(
            id = "whisper-tiny-q5_1",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper tiny (multilingual, q5_1)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin",
            sha256 = "818710568da3ca15689e31a743197b520007872ff9576237bda97bd1b469c3d7",
            sizeBytes = 32_152_673,
            archive = false,
            license = "MIT",
        ),
        ModelSpec(
            id = "whisper-base-q5_1",
            kind = ModelKind.WHISPER,
            language = null,
            displayName = "Whisper base (multilingual, q5_1)",
            url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin",
            sha256 = "422f1ae452ade6f30a004d7e5c6a43195e4433bc370bf23fac9cc591f01a8898",
            sizeBytes = 59_707_625,
            archive = false,
            license = "MIT",
        ),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): ModelSpec? = byId[id]

    fun vosk(language: VoiceLanguage): ModelSpec =
        all.first { it.kind == ModelKind.VOSK && it.language == language }

    val whisper: List<ModelSpec> get() = all.filter { it.kind == ModelKind.WHISPER }
}
