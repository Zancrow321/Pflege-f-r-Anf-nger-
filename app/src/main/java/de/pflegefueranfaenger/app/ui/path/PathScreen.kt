package de.pflegefueranfaenger.app.ui.path

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.app.LoadedContent
import de.pflegefueranfaenger.app.ui.components.Pill
import de.pflegefueranfaenger.app.ui.components.StatsRow
import de.pflegefueranfaenger.core.game.Progress
import de.pflegefueranfaenger.core.game.ProgressRules
import de.pflegefueranfaenger.core.model.Level
import java.time.LocalDate
import kotlin.math.sin

/** Lernpfad: Einheiten mit Lektionen als Knoten im Zickzack, wie ein Weg. */
@Composable
fun PathScreen(
    content: LoadedContent,
    progress: Progress,
    hearts: Int,
    onOpenLesson: (String) -> Unit,
    onStartReview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bundle = content.bundle
    val today = LocalDate.now()
    val next = ProgressRules.nextLesson(progress, bundle)
    val xpToday = ProgressRules.xpToday(progress, today)

    LazyColumn(modifier, contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pflege für Anfänger", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                StatsRow(ProgressRules.currentStreak(progress, today), progress.xp, hearts)
                Text("Tagesziel: $xpToday / ${progress.dailyGoalXp} XP", style = MaterialTheme.typography.labelLarge)
                LinearProgressIndicator(
                    progress = { (xpToday.toFloat() / progress.dailyGoalXp).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                )
                if (progress.mistakes.isNotEmpty()) {
                    OutlinedButton(onClick = onStartReview, modifier = Modifier.fillMaxWidth()) {
                        Text("Fehler wiederholen (${progress.mistakes.size})")
                    }
                }
            }
        }
        var index = 0
        bundle.units.forEachIndexed { unitNo, unit ->
            item(key = unit.id) {
                val done = unit.lessons.count { it.id in progress.completedLessons }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (unit.level == Level.GRUNDLAGEN) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Einheit ${unitNo + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            if (unit.level == Level.GRUNDLAGEN) {
                                Pill("Grundlagen", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                        Text(unit.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(unit.description, style = MaterialTheme.typography.bodyMedium)
                        Text("$done von ${unit.lessons.size} Lektionen", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            unit.lessons.forEach { lesson ->
                val i = index++
                item(key = lesson.id) {
                    val completed = lesson.id in progress.completedLessons
                    val unlocked = ProgressRules.isUnlocked(progress, bundle, lesson.id)
                    val isNext = lesson.id == next
                    LessonNode(
                        title = lesson.title,
                        state = when {
                            isNext -> NodeState.NEXT
                            completed -> NodeState.DONE
                            unlocked -> NodeState.OPEN
                            else -> NodeState.LOCKED
                        },
                        offsetFraction = sin(i * 0.9).toFloat(),
                        onClick = { if (unlocked || completed) onOpenLesson(lesson.id) },
                    )
                }
            }
        }
    }
}

private enum class NodeState { DONE, NEXT, OPEN, LOCKED }

@Composable
private fun LessonNode(title: String, state: NodeState, offsetFraction: Float, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg, symbol) = when (state) {
        NodeState.DONE -> Triple(scheme.secondary, scheme.onSecondary, "✓")
        NodeState.NEXT -> Triple(scheme.primary, scheme.onPrimary, "★")
        NodeState.OPEN -> Triple(scheme.primaryContainer, scheme.onPrimaryContainer, "▶")
        NodeState.LOCKED -> Triple(scheme.surfaceVariant, scheme.onSurfaceVariant, "🔒")
    }
    val stateText = when (state) {
        NodeState.DONE -> "abgeschlossen"
        NodeState.NEXT -> "als Nächstes"
        NodeState.OPEN -> "freigeschaltet"
        NodeState.LOCKED -> "gesperrt"
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.offset(x = (offsetFraction * 70).dp).width(180.dp).semantics { contentDescription = "$title, $stateText" },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                onClick = onClick,
                enabled = state != NodeState.LOCKED,
                shape = CircleShape,
                color = bg,
                contentColor = fg,
                border = if (state == NodeState.NEXT) BorderStroke(4.dp, scheme.primaryContainer) else null,
                shadowElevation = if (state == NodeState.LOCKED) 0.dp else 4.dp,
                modifier = Modifier.size(if (state == NodeState.NEXT) 80.dp else 68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(symbol, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            if (state == NodeState.NEXT) {
                Surface(color = scheme.primary, contentColor = scheme.onPrimary, shape = RoundedCornerShape(8.dp)) {
                    Text("Start", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (state == NodeState.LOCKED) scheme.onSurfaceVariant else scheme.onSurface,
            )
        }
    }
}
