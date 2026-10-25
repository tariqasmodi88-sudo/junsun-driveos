package it.driveos.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import it.driveos.launcher.settings.ThemeMode

val AccentColors = listOf(
    Color(0xFF4FC3F7), // azzurro
    Color(0xFF81C784), // verde
    Color(0xFFFFB74D), // ambra
    Color(0xFFE57373), // rosso
    Color(0xFFB39DDB), // viola
)
val AccentNames = listOf("Azzurro", "Verde", "Ambra", "Rosso", "Viola")

private fun darker(c: Color) = Color(red = c.red * 0.55f, green = c.green * 0.55f, blue = c.blue * 0.55f, alpha = 1f)

@Composable
fun DriveOSTheme(themeMode: ThemeMode, accentIndex: Int, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val accent = AccentColors.getOrElse(accentIndex) { AccentColors[0] }
    val colors = if (dark) {
        darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF06080A),
            secondary = accent,
            background = Color(0xFF040507),
            onBackground = Color(0xFFECEFF3),
            surface = Color(0xFF101318),
            onSurface = Color(0xFFECEFF3),
            surfaceVariant = Color(0xFF1B1F26),
            onSurfaceVariant = Color(0xFFA7B0BB),
            outline = Color(0xFF2B313A),
            error = Color(0xFFFF6E6E),
        )
    } else {
        lightColorScheme(
            primary = darker(accent),
            onPrimary = Color.White,
            secondary = darker(accent),
            background = Color(0xFFEFF2F6),
            onBackground = Color(0xFF0F1318),
            surface = Color.White,
            onSurface = Color(0xFF0F1318),
            surfaceVariant = Color(0xFFE1E6EC),
            onSurfaceVariant = Color(0xFF47525E),
            outline = Color(0xFFC7CED7),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
