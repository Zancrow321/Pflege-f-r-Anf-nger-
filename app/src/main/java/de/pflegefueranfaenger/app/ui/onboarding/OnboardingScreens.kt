package de.pflegefueranfaenger.app.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import de.pflegefueranfaenger.app.R
import de.pflegefueranfaenger.app.ui.theme.LogoNavy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

const val DISCLAIMER =
    "Diese App ist eine Lernhilfe. Sie ersetzt weder die Einarbeitung auf deiner Station noch die Standards (SOPs) " +
        "deines Hauses oder ärztliche Anordnungen. Die Inhalte stützen sich auf Leitlinien und Fachliteratur, die jeweils " +
        "als Quelle angegeben sind. Sie sind jedoch nicht fachlich geprüft. Grenzwerte, Abläufe und Medikamente können " +
        "sich je nach Klinik unterscheiden. Im Zweifel gilt immer der Standard deines Hauses."

@Composable
fun DisclaimerScreen(onAccept: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = "Logo Pflege für Anfänger",
            modifier = Modifier.padding(top = 32.dp).size(120.dp).clip(RoundedCornerShape(28.dp)).background(LogoNavy),
        )
        Text("Pflege für Anfänger", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Lerne in kurzen Lektionen alles für den Start auf der kardiologischen Telemetrie-Station: Fachsprache, EKG, Rhythmen, Alarme und Notfälle. Dazu gibt es ein Nachschlagewerk zum schnellen Nachlesen.",
            textAlign = TextAlign.Center,
        )
        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Wichtiger Hinweis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(DISCLAIMER, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = onAccept, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Verstanden") }
    }
}

@Composable
fun PlacementChoiceScreen(onStartFromScratch: () -> Unit, onPlacementTest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("Wo möchtest du starten?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedCard(onClick = onStartFromScratch, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🌱  Von Anfang an", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Für Auszubildende und alle, die neu in der Pflege sind. Du beginnst mit Fachsprache und den Grundlagen zu Herz und Kreislauf.")
            }
        }
        OutlinedCard(onClick = onPlacementTest, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🎯  Einstiegstest machen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Für examinierte Pflegekräfte, die neu auf der Station sind. 10 Fragen; ab 80 % überspringst du die Grundlagen.")
            }
        }
    }
}
