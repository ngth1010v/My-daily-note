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

    val swatches = listOf(
        Swatch("teal", Color(0xFF00897B), Color(0xFF4DB6AC)),
        Swatch("blue", Color(0xFF1E88E5), Color(0xFF64B5F6)),
        Swatch("indigo", Color(0xFF3949AB), Color(0xFF7986CB)),
        Swatch("purple", Color(0xFF8E24AA), Color(0xFFBA68C8)),
        Swatch("pink", Color(0xFFD81B60), Color(0xFFF06292)),
        Swatch("red", Color(0xFFE53935), Color(0xFFEF5350)),
        Swatch("orange", Color(0xFFF4511E), Color(0xFFFF8A65)),
        Swatch("amber", Color(0xFFFFB300), Color(0xFFFFD54F)),
        Swatch("green", Color(0xFF43A047), Color(0xFF81C784)),
        Swatch("gray", Color(0xFF757575), Color(0xFFBDBDBD)),
    )
    private val byKey = swatches.associateBy { it.key }

    fun color(key: String?, fallbackKey: String, dark: Boolean): Color {
        val s = byKey[key] ?: byKey[fallbackKey] ?: swatches.first()
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
