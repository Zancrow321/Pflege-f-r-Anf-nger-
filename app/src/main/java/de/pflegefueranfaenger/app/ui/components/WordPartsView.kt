package de.pflegefueranfaenger.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.core.model.WordPart

/** Farben für die einzelnen Bausteine (Vorsilbe, Stamm, Endung …). */
val PartColors = listOf(
    Color(0xFFE3F2FD) to Color(0xFF0D47A1),
    Color(0xFFFCE4EC) to Color(0xFF880E4F),
    Color(0xFFE8F5E9) to Color(0xFF1B5E20),
    Color(0xFFFFF3E0) to Color(0xFFE65100),
)

/** Zeigt einen Fachbegriff zerlegt in Bausteine, mit Einzelbedeutung und Herkunft. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordPartsBreakdown(parts: List<WordPart>, modifier: Modifier = Modifier, showOrigin: Boolean = true) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { i, p ->
            val (bg, fg) = PartColors[i % PartColors.size]
            Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp).widthIn(min = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(p.part, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(p.meaning, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                    val origin = p.origin
                    if (showOrigin && origin != null) {
                        Text(origin, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
