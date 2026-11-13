package de.pflegefueranfaenger.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HeartRed = Color(0xFFC62828)
val CorrectGreen = Color(0xFF2E7D32)
val WrongRed = Color(0xFFC62828)
val StreakOrange = Color(0xFFEF6C00)
val XpGold = Color(0xFFF9A825)

/** Logo-Farben: Dunkelblau und Türkis. */
val LogoNavy = Color(0xFF011753)
val LogoTurquoise = Color(0xFF38F5C3)

private val Light = lightColorScheme(
    primary = LogoNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF00164F),
    secondary = Color(0xFF006B55),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB4F5E1),
    onSecondaryContainer = Color(0xFF002118),
    tertiary = Color(0xFF3949AB),
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF),
)

private val Dark = darkColorScheme(
    primary = LogoTurquoise,
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF1C2E6B),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFF8FD8C2),
    onSecondary = Color(0xFF00382B),
    secondaryContainer = Color(0xFF005141),
    onSecondaryContainer = Color(0xFFB4F5E1),
    tertiary = Color(0xFF9FA8DA),
    background = Color(0xFF060D2B),
    surface = Color(0xFF060D2B),
)

@Composable
fun PflegeTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
