package io.github.ngth1010v.mydailynote.core.data.autostyle

import io.github.ngth1010v.mydailynote.core.data.model.TextNormalizer
import kotlin.math.abs
import kotlin.random.Random

data class Style(val color: String, val icon: String)

/** A past action's text with the color and icon it is shown with. */
data class StyledText(val text: String, val color: String, val icon: String)

/** An icon the picker may choose: [meaning] is matched against action texts; [group] is its picker section. */
class IconMeaning(val key: String, val group: Int, val meaning: String)

/**
 * Picks a color and icon for a newly typed action from how related it is to past actions:
 * - same text (ignoring case and accents): the same color and icon;
 * - very related: the same icon, same hue one shade away;
 * - related: the best-matching icon in the related action's icon group, same hue 2+ shades away or
 *   a neighbouring hue;
 * - unrelated, or no past actions: the best-matching icon overall, and the hue used least so far
 *   (random when there is no history).
 */
class AutoStyle(
    private val embed: (String) -> FloatArray,
    icons: List<IconMeaning>,
    /** Palette keys as [row][hue column]; rows go from light to dark shades. */
    private val palette: List<List<String>>,
    private val random: Random = Random.Default,
) {
    private val icons = icons.map { it to embed(it.meaning) }
    private val iconsByKey = icons.associateBy { it.key }
    private val cells = palette.flatMapIndexed { r, row -> row.mapIndexed { c, key -> key to (r to c) } }.toMap()
    private val cache = HashMap<String, FloatArray>()
    private val normalizedCache = HashMap<String, String>()

    @Synchronized
    fun pick(text: String, history: List<StyledText>): Style {
        val normalized = normalized(text)
        history.firstOrNull { normalized(it.text) == normalized }?.let { return Style(it.color, it.icon) }

        val v = vector(text)
        val nearest = history.maxByOrNull { dot(v, vector(it.text)) }
        val similarity = nearest?.let { dot(v, vector(it.text)) } ?: 0f
        return when {
            nearest != null && similarity >= VERY_RELATED ->
                Style(shifted(nearest.color, near = true) ?: freshColor(history), nearest.icon)
            nearest != null && similarity >= RELATED -> {
                val group = iconsByKey[nearest.icon]?.group
                Style(shifted(nearest.color, near = false) ?: freshColor(history), bestIcon(v) { it.group == group } ?: bestIcon(v) { true }!!)
            }
            else -> Style(freshColor(history), bestIcon(v) { true }!!)
        }
    }

    private fun vector(text: String) = cache.getOrPut(text) { embed(text) }

    private fun normalized(text: String) = normalizedCache.getOrPut(text) { TextNormalizer.normalize(text) }

    private fun bestIcon(v: FloatArray, allowed: (IconMeaning) -> Boolean): String? =
        icons.filter { allowed(it.first) }.maxByOrNull { dot(v, it.second) }?.first?.key

    /** A palette cell close to [color]: one shade away ([near]), or 2+ shades / a neighbouring hue. */
    private fun shifted(color: String, near: Boolean): String? {
        val (row, col) = cells[color] ?: return null
        val candidates = READABLE_ROWS.flatMap { r -> palette[r].indices.map { c -> r to c } }.filter { (r, c) ->
            if (near) c == col && abs(r - row) == 1
            else (c == col && abs(r - row) >= 2) || (r == row && abs(c - col) == 1)
        }
        return candidates.randomOrNull(random)?.let { (r, c) -> palette[r][c] }
    }

    /** The least used hue (random among ties), in a mid shade. */
    private fun freshColor(history: List<StyledText>): String {
        val used = IntArray(palette.first().size)
        history.forEach { h -> cells[h.color]?.let { used[it.second]++ } }
        val least = used.min()
        val col = used.indices.filter { used[it] == least }.random(random)
        return palette[(1..3).random(random)][col]
    }

    private fun dot(a: FloatArray, b: FloatArray): Float {
        var s = 0f
        for (i in a.indices) s += a[i] * b[i]
        return s
    }

    companion object {
        // Calibrated on English/Vietnamese pairs. Same action in other words: "Tập gym"/"Go to gym" 0.93,
        // "Gọi mẹ"/"Call mom" 0.82, "Drink coffee"/"Uống cà phê" 0.78. A shared verb alone stays below:
        // "Uống trà"/"Drink coffee" 0.74, "Uống bia"/"Uống cà phê" 0.60. Related: "Meditate"/"Thiền" 0.65,
        // "Morning run"/"Chạy bộ" 0.39, "Đi chợ"/"Đi siêu thị" 0.36. Unrelated actions ~0.2.
        const val VERY_RELATED = 0.75f
        const val RELATED = 0.35f
        /** The lightest row is too pale for an icon on the light background. */
        private val READABLE_ROWS = 1..4
    }
}
