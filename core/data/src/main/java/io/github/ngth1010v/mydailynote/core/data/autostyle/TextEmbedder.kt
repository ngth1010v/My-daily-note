package io.github.ngth1010v.mydailynote.core.data.autostyle

import java.io.InputStream
import java.nio.ByteBuffer
import java.text.Normalizer
import kotlin.math.sqrt

/**
 * Static sentence embeddings (Model2Vec, distilled from BAAI/bge-m3) cut down to English and
 * Vietnamese; built by tools/autostyle/build_model.py. A text's vector is the normalized mean of
 * its SentencePiece (Unigram) pieces' rows, so related texts point the same way:
 * "Gọi mẹ" · "Call mom" ≈ 0.8, unrelated actions ≈ 0.2.
 */
class TextEmbedder(input: InputStream) {
    private val ids = HashMap<String, Int>(1 shl 17)
    private val scores: FloatArray
    private val scales: FloatArray
    /** All rows, int8, starting at [rowsOffset]. */
    private val rows: ByteArray
    private val rowsOffset: Int
    private val unkId: Int
    private val unkScore: Float
    private var maxPieceLength = 1
    val dims: Int

    init {
        // One read, then parse in place: much faster than field-by-field stream reads.
        rows = input.use { it.readBytes() }
        val b = ByteBuffer.wrap(rows)
        require(String(rows, 0, 4, Charsets.US_ASCII) == "MDN1") { "Not an embeddings file" }
        b.position(4)
        val count = b.int
        dims = b.short.toInt() and 0xFFFF
        unkId = b.int
        unkScore = b.float
        scores = FloatArray(count)
        scales = FloatArray(count)
        for (i in 0 until count) {
            val length = b.get().toInt() and 0xFF
            val piece = String(rows, b.position(), length, Charsets.UTF_8)
            b.position(b.position() + length)
            ids[piece] = i
            maxPieceLength = maxOf(maxPieceLength, piece.length)
            scores[i] = b.float
            scales[i] = b.float
        }
        rowsOffset = b.position()
    }

    /** Unit vector for [text]; all zeros when nothing in it is known. */
    fun embed(text: String): FloatArray {
        val sum = FloatArray(dims)
        var n = 0
        for (id in tokenize(text)) {
            if (id == unkId) continue
            val scale = scales[id]
            val base = rowsOffset + id * dims
            for (d in 0 until dims) sum[d] += rows[base + d] * scale
            n++
        }
        val norm = sqrt(sum.sumOf { (it * it).toDouble() }).toFloat()
        if (n == 0 || norm == 0f) return sum
        for (d in 0 until dims) sum[d] /= norm
        return sum
    }

    /** Most likely piece segmentation (Viterbi), the same as the source tokenizer's. */
    internal fun tokenize(text: String): List<Int> {
        val s = "▁" + normalize(text).ifEmpty { return emptyList() }.replace(' ', '▁')
        val best = DoubleArray(s.length + 1) { Double.NEGATIVE_INFINITY }.also { it[0] = 0.0 }
        val from = IntArray(s.length + 1)
        val piece = IntArray(s.length + 1)
        for (start in s.indices) {
            if (best[start] == Double.NEGATIVE_INFINITY) continue
            var single = false
            for (end in start + 1..minOf(s.length, start + maxPieceLength)) {
                val id = ids[s.substring(start, end)] ?: continue
                if (end == start + 1) single = true
                val score = best[start] + scores[id]
                if (score > best[end]) { best[end] = score; from[end] = start; piece[end] = id }
            }
            if (!single && best[start] + unkScore > best[start + 1]) { // unknown character
                best[start + 1] = best[start] + unkScore; from[start + 1] = start; piece[start + 1] = unkId
            }
        }
        val out = ArrayList<Int>()
        var end = s.length
        while (end > 0) { out += piece[end]; end = from[end] }
        return out.asReversed()
    }

    internal fun piece(id: Int): String? = ids.entries.firstOrNull { it.value == id }?.key

    companion object {
        private const val PUNCTUATION = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"
        private val spaces = Regex("\\s+")

        /** Lowercase (the model matches much better that way), NFKC, punctuation split off. */
        internal fun normalize(text: String): String {
            val sb = StringBuilder()
            for (c in Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKC)) {
                if (c in PUNCTUATION) sb.append(' ').append(c).append(' ') else sb.append(c)
            }
            return sb.toString().replace(spaces, " ").trim()
        }
    }
}
