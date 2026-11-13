package de.pflegefueranfaenger.app.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.app.LoadedContent
import de.pflegefueranfaenger.app.ui.lookup.Sources
import de.pflegefueranfaenger.app.ui.onboarding.DISCLAIMER
import de.pflegefueranfaenger.core.game.Progress
import de.pflegefueranfaenger.core.game.ProgressRules
import java.time.LocalDate

@Composable
fun ProfileScreen(
    content: LoadedContent,
    progress: Progress,
    onSetDailyGoal: (Int) -> Unit,
    onPlacementTest: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bundle = content.bundle
    var confirmReset by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Profil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StatLine("🔥 Aktuelle Serie", "${ProgressRules.currentStreak(progress, today)} Tage")
                StatLine("🏅 Längste Serie", "${progress.longestStreak} Tage")
                StatLine("⭐ Gesamt-XP", progress.xp.toString())
                StatLine("📚 Lektionen", "${progress.completedLessons.count { bundle.lesson(it) != null }} / ${bundle.lessons.size}")
                StatLine("🔁 Offene Fehler", progress.mistakes.size.toString())
            }
        }
        Text("Tagesziel", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(10, 20, 30, 50).forEach { xp ->
                FilterChip(selected = progress.dailyGoalXp == xp, onClick = { onSetDailyGoal(xp) }, label = { Text("$xp XP") })
            }
        }
        OutlinedButton(onClick = onPlacementTest, modifier = Modifier.fillMaxWidth()) { Text("Einstiegstest (erneut) machen") }

        Text("Hinweis", style = MaterialTheme.typography.titleMedium)
        Text(DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
        Text("Alle Quellen", style = MaterialTheme.typography.titleMedium)
        Sources(bundle.sources, "Quellenverzeichnis")

        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Fortschritt zurücksetzen", color = MaterialTheme.colorScheme.error)
        }
        Text("Version 0.1 · Inhalte Stand 13.11.2026", style = MaterialTheme.typography.labelSmall)
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Fortschritt zurücksetzen?") },
            text = { Text("XP, Serie, abgeschlossene Lektionen und Fehler werden gelöscht. Das lässt sich nicht rückgängig machen.") },
            confirmButton = { TextButton(onClick = { confirmReset = false; onReset() }) { Text("Zurücksetzen") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}
