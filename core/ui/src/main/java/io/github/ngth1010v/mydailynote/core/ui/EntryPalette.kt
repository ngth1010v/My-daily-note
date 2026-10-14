package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.ui.graphics.Color

/**
 * Entry colors are stored as palette keys (not ARGB). Unknown keys (e.g. from a newer backup) fall
 * back to teal.
 */
object EntryPalette {
    data class Swatch(val key: String, val color: Color)

    /** Material hues, one grid column each; values are shades 100, 200, 300, 400, 500, 600, 800, 900. */
    private val hues: List<Pair<String, LongArray>> = listOf(
        "red" to longArrayOf(0xFFFFCDD2, 0xFFEF9A9A, 0xFFE57373, 0xFFEF5350, 0xFFF44336, 0xFFE53935, 0xFFC62828, 0xFFB71C1C),
        "pink" to longArrayOf(0xFFF8BBD0, 0xFFF48FB1, 0xFFF06292, 0xFFEC407A, 0xFFE91E63, 0xFFD81B60, 0xFFAD1457, 0xFF880E4F),
        "purple" to longArrayOf(0xFFE1BEE7, 0xFFCE93D8, 0xFFBA68C8, 0xFFAB47BC, 0xFF9C27B0, 0xFF8E24AA, 0xFF6A1B9A, 0xFF4A148C),
        "deeppurple" to longArrayOf(0xFFD1C4E9, 0xFFB39DDB, 0xFF9575CD, 0xFF7E57C2, 0xFF673AB7, 0xFF5E35B1, 0xFF4527A0, 0xFF311B92),
        "indigo" to longArrayOf(0xFFC5CAE9, 0xFF9FA8DA, 0xFF7986CB, 0xFF5C6BC0, 0xFF3F51B5, 0xFF3949AB, 0xFF283593, 0xFF1A237E),
        "blue" to longArrayOf(0xFFBBDEFB, 0xFF90CAF9, 0xFF64B5F6, 0xFF42A5F5, 0xFF2196F3, 0xFF1E88E5, 0xFF1565C0, 0xFF0D47A1),
        "lightblue" to longArrayOf(0xFFB3E5FC, 0xFF81D4FA, 0xFF4FC3F7, 0xFF29B6F6, 0xFF03A9F4, 0xFF039BE5, 0xFF0277BD, 0xFF01579B),
        "cyan" to longArrayOf(0xFFB2EBF2, 0xFF80DEEA, 0xFF4DD0E1, 0xFF26C6DA, 0xFF00BCD4, 0xFF00ACC1, 0xFF00838F, 0xFF006064),
        "teal" to longArrayOf(0xFFB2DFDB, 0xFF80CBC4, 0xFF4DB6AC, 0xFF26A69A, 0xFF009688, 0xFF00897B, 0xFF00695C, 0xFF004D40),
        "green" to longArrayOf(0xFFC8E6C9, 0xFFA5D6A7, 0xFF81C784, 0xFF66BB6A, 0xFF4CAF50, 0xFF43A047, 0xFF2E7D32, 0xFF1B5E20),
        "lime" to longArrayOf(0xFFF0F4C3, 0xFFE6EE9C, 0xFFDCE775, 0xFFD4E157, 0xFFCDDC39, 0xFFC0CA33, 0xFF9E9D24, 0xFF827717),
        "amber" to longArrayOf(0xFFFFECB3, 0xFFFFE082, 0xFFFFD54F, 0xFFFFCA28, 0xFFFFC107, 0xFFFFB300, 0xFFFF8F00, 0xFFFF6F00),
        "orange" to longArrayOf(0xFFFFE0B2, 0xFFFFCC80, 0xFFFFB74D, 0xFFFFA726, 0xFFFF9800, 0xFFFB8C00, 0xFFEF6C00, 0xFFE65100),
        "deeporange" to longArrayOf(0xFFFFCCBC, 0xFFFFAB91, 0xFFFF8A65, 0xFFFF7043, 0xFFFF5722, 0xFFF4511E, 0xFFD84315, 0xFFBF360C),
        "gray" to longArrayOf(0xFFF5F5F5, 0xFFEEEEEE, 0xFFE0E0E0, 0xFFBDBDBD, 0xFF9E9E9E, 0xFF757575, 0xFF424242, 0xFF212121),
    )
    private val shades = intArrayOf(100, 200, 300, 400, 500, 600, 800, 900)

    /** Shade of each grid row. */
    private val rows = listOf(200, 400, 600, 800, 900)
    // Gray's light shades vanish on a light surface, so its column starts darker.
    private val grayRows = listOf(400, 500, 600, 800, 900)

    /** The original 10 colors keep their stored keys (and exact values), in the 600 row of their column. */
    private val legacy = mapOf(
        "teal" to Swatch("teal", Color(0xFF00897B)),
        "blue" to Swatch("blue", Color(0xFF1E88E5)),
        "indigo" to Swatch("indigo", Color(0xFF3949AB)),
        "purple" to Swatch("purple", Color(0xFF8E24AA)),
        "pink" to Swatch("pink", Color(0xFFD81B60)),
        "red" to Swatch("red", Color(0xFFE53935)),
        "deeporange" to Swatch("orange", Color(0xFFF4511E)),
        "amber" to Swatch("amber", Color(0xFFFFB300)),
        "green" to Swatch("green", Color(0xFF43A047)),
        "gray" to Swatch("gray", Color(0xFF757575)),
    )

    const val COLUMNS = 15
    const val ROWS = 5

    /** Row-major [ROWS] x [COLUMNS] grid; new colors are keyed "<hue>-<shade>". */
    val swatches: List<Swatch> = (0 until ROWS).flatMap { r ->
        hues.map { (hue, values) ->
            val shade = (if (hue == "gray") grayRows else rows)[r]
            legacy[hue]?.takeIf { shade == 600 } ?: Swatch("$hue-$shade", Color(values[shades.indexOf(shade)]))
        }
    }
    private val byKey = swatches.associateBy { it.key }

    fun color(key: String?): Color = (byKey[key] ?: legacy.getValue("teal")).color
}
