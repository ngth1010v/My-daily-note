package io.github.ngth1010v.mydailynote.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Entry colors are stored as palette keys (not ARGB) so each key can have a light and a dark
 * variant. Unknown keys (e.g. from a newer backup) fall back to the default color.
 */
object EntryPalette {
    data class Swatch(val key: String, val light: Color, val dark: Color)

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

    /** Grid rows as (light shade, dark-theme shade): darker rows get a lighter variant on dark surfaces. */
    private val rows = listOf(200 to 100, 400 to 200, 600 to 300, 800 to 400, 900 to 500)
    // Gray's light shades vanish on a light surface, so its column starts darker.
    private val grayRows = listOf(400 to 300, 500 to 400, 600 to 400, 800 to 500, 900 to 600)

    /** The original 10 colors keep their stored keys (and exact values), in the 600 row of their column. */
    private val legacy = mapOf(
        "teal" to Swatch("teal", Color(0xFF00897B), Color(0xFF4DB6AC)),
        "blue" to Swatch("blue", Color(0xFF1E88E5), Color(0xFF64B5F6)),
        "indigo" to Swatch("indigo", Color(0xFF3949AB), Color(0xFF7986CB)),
        "purple" to Swatch("purple", Color(0xFF8E24AA), Color(0xFFBA68C8)),
        "pink" to Swatch("pink", Color(0xFFD81B60), Color(0xFFF06292)),
        "red" to Swatch("red", Color(0xFFE53935), Color(0xFFEF5350)),
        "deeporange" to Swatch("orange", Color(0xFFF4511E), Color(0xFFFF8A65)),
        "amber" to Swatch("amber", Color(0xFFFFB300), Color(0xFFFFD54F)),
        "green" to Swatch("green", Color(0xFF43A047), Color(0xFF81C784)),
        "gray" to Swatch("gray", Color(0xFF757575), Color(0xFFBDBDBD)),
    )

    const val COLUMNS = 15
    const val ROWS = 5

    /** Row-major [ROWS] x [COLUMNS] grid; new colors are keyed "<hue>-<shade>". */
    val swatches: List<Swatch> = (0 until ROWS).flatMap { r ->
        hues.map { (hue, values) ->
            val (lightShade, darkShade) = (if (hue == "gray") grayRows else rows)[r]
            legacy[hue]?.takeIf { lightShade == 600 }
                ?: Swatch(
                    "$hue-$lightShade",
                    Color(values[shades.indexOf(lightShade)]),
                    Color(values[shades.indexOf(darkShade)]),
                )
        }
    }
    private val byKey = swatches.associateBy { it.key }

    fun color(key: String?, fallbackKey: String, dark: Boolean): Color {
        val s = byKey[key] ?: byKey[fallbackKey] ?: legacy.getValue("teal")
        return if (dark) s.dark else s.light
    }
}

/** Icon keys stored with entries, mapped to Material icons. */
object EntryIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "check" to Icons.Filled.CheckCircle,
        "water" to Icons.Filled.LocalDrink,
        "coffee" to Icons.Filled.Coffee,
        "food" to Icons.Filled.Restaurant,
        "fitness" to Icons.Filled.FitnessCenter,
        "run" to Icons.AutoMirrored.Filled.DirectionsRun,
        "walk" to Icons.AutoMirrored.Filled.DirectionsWalk,
        "bike" to Icons.AutoMirrored.Filled.DirectionsBike,
        "book" to Icons.AutoMirrored.Filled.MenuBook,
        "study" to Icons.Filled.School,
        "work" to Icons.Filled.Work,
        "code" to Icons.Filled.Code,
        "sleep" to Icons.Filled.Bedtime,
        "wake" to Icons.Filled.WbSunny,
        "meditate" to Icons.Filled.SelfImprovement,
        "music" to Icons.Filled.MusicNote,
        "call" to Icons.Filled.Call,
        "chat" to Icons.AutoMirrored.Filled.Chat,
        "mail" to Icons.Filled.Email,
        "shopping" to Icons.Filled.ShoppingCart,
        "money" to Icons.Filled.Payments,
        "clean" to Icons.Filled.CleaningServices,
        "home" to Icons.Filled.Home,
        "car" to Icons.Filled.DirectionsCar,
        "pill" to Icons.Filled.Medication,
        "heart" to Icons.Filled.Favorite,
        "star" to Icons.Filled.Star,
        "idea" to Icons.Filled.Lightbulb,
        "pet" to Icons.Filled.Pets,
        "plant" to Icons.Filled.LocalFlorist,
        "game" to Icons.Filled.SportsEsports,
        "movie" to Icons.Filled.Movie,
    )
    private val byKey = all.toMap()

    fun icon(key: String?): ImageVector? = key?.let(byKey::get)
}
