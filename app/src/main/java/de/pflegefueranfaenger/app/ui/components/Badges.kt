package de.pflegefueranfaenger.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.app.ui.theme.HeartRed
import de.pflegefueranfaenger.app.ui.theme.StreakOrange
import de.pflegefueranfaenger.app.ui.theme.XpGold

@Composable
fun Pill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** Hinweis, dass Inhalte nicht fachlich geprüft sind (Entscheidung: nur Quellen, keine Prüfung). */
@Composable
fun UnreviewedBadge(modifier: Modifier = Modifier) =
    Pill("Nicht fachlich geprüft", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, modifier)

@Composable
fun HouseSpecificBadge(modifier: Modifier = Modifier) =
    Pill("Hausstandard beachten", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, modifier)

/** Kopfzeile mit Streak, XP und Herzen. */
@Composable
fun StatsRow(streak: Int, xp: Int, hearts: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Stat("🔥", streak.toString(), StreakOrange, "Tage in Folge")
        Stat("⭐", xp.toString(), XpGold, "XP")
        Stat("❤", hearts.toString(), HeartRed, "Herzen")
    }
}

@Composable
private fun Stat(icon: String, value: String, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, style = MaterialTheme.typography.titleMedium)
        Text(
            " $value",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
