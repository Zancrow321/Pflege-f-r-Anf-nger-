package de.pflegefueranfaenger.app.ui.lesson

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.app.ui.components.EcgStrip
import de.pflegefueranfaenger.app.ui.components.PartColors
import de.pflegefueranfaenger.core.game.Answer
import de.pflegefueranfaenger.core.model.Question

/**
 * Zeigt eine Frage an. [onAnswerChanged] liefert die aktuelle (ggf. unvollständige) Antwort
 * oder null, solange noch nicht geprüft werden kann. [locked] sperrt die Eingabe nach dem Prüfen.
 */
@Composable
fun QuestionView(question: Question, locked: Boolean, onAnswerChanged: (Answer?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        question.strip?.let { EcgStrip(it) }
        when (question) {
            is Question.SingleChoice -> SingleChoiceView(question, locked, onAnswerChanged)
            is Question.TrueFalse -> TrueFalseView(question, locked, onAnswerChanged)
            is Question.Matching -> MatchingView(question, locked, onAnswerChanged)
            is Question.WordParts -> WordPartsQuestionView(question, locked, onAnswerChanged)
        }
    }
}

@Composable
private fun Prompt(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ChoiceCard(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    OutlinedCard(
        onClick = onClick,
        enabled = enabled,
        border = border,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun SingleChoiceView(q: Question.SingleChoice, locked: Boolean, onAnswer: (Answer?) -> Unit) {
    // Antworten gemischt anzeigen; gemerkt wird der Original-Index
    val order = remember(q.id) { q.options.indices.shuffled() }
    var selected by remember(q.id) { mutableStateOf<Int?>(null) }
    Prompt(q.prompt)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        order.forEach { idx ->
            ChoiceCard(q.options[idx], selected == idx, !locked, onClick = {
                selected = idx
                onAnswer(Answer.Choice(idx))
            })
        }
    }
}

@Composable
private fun TrueFalseView(q: Question.TrueFalse, locked: Boolean, onAnswer: (Answer?) -> Unit) {
    var selected by remember(q.id) { mutableStateOf<Boolean?>(null) }
    Text("Richtig oder falsch?", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Prompt(q.statement)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(true to "Richtig", false to "Falsch").forEach { (value, label) ->
            ChoiceCard(label, selected == value, !locked, onClick = {
                selected = value
                onAnswer(Answer.Bool(value))
            }, modifier = Modifier.weight(1f))
        }
    }
}

/** Zuordnen: links antippen, dann rechts antippen. Ein verbundenes Paar lässt sich durch erneutes Antippen lösen. */
@Composable
private fun MatchingView(q: Question.Matching, locked: Boolean, onAnswer: (Answer?) -> Unit) {
    val lefts = remember(q.id) { q.pairs.map { it.left } }
    val rights = remember(q.id) { q.pairs.map { it.right }.shuffled() }
    val links = remember(q.id) { mutableStateMapOf<String, String>() } // left -> right
    var activeLeft by remember(q.id) { mutableStateOf<String?>(null) }

    fun publish() = onAnswer(if (links.size == lefts.size) Answer.Matches(links.toMap()) else null)
    fun colorOf(left: String): Pair<Color, Color>? = lefts.indexOf(left).takeIf { links.containsKey(left) }?.let { PartColors[it % PartColors.size] }

    Prompt(q.prompt)
    Text("Tippe links einen Begriff an und dann rechts die passende Bedeutung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lefts.forEach { left ->
                val c = colorOf(left)
                MatchTile(left, c, highlighted = activeLeft == left, enabled = !locked) {
                    if (links.containsKey(left)) {
                        links.remove(left); publish()
                    }
                    activeLeft = if (activeLeft == left) null else left
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rights.forEach { right ->
                val owner = links.entries.firstOrNull { it.value == right }?.key
                MatchTile(right, owner?.let { colorOf(it) }, highlighted = false, enabled = !locked) {
                    val left = activeLeft
                    if (left != null) {
                        links.keys.filter { links[it] == right }.forEach { links.remove(it) }
                        links[left] = right
                        activeLeft = null
                        publish()
                    } else if (owner != null) {
                        links.remove(owner); publish()
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchTile(text: String, colors: Pair<Color, Color>?, highlighted: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val border = when {
        highlighted -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        colors != null -> BorderStroke(2.dp, colors.second)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = border,
        color = colors?.first ?: MaterialTheme.colorScheme.surface,
        contentColor = colors?.second ?: MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
    }
}

/**
 * Wortbausteine: Oben steht der Begriff in Bausteine zerlegt, darunter je ein freies Feld.
 * Aus der Auswahl unten wird für jeden Baustein die Einzelbedeutung gewählt.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordPartsQuestionView(q: Question.WordParts, locked: Boolean, onAnswer: (Answer?) -> Unit) {
    val bank = remember(q.id) { (q.parts.map { it.meaning } + q.distractors).distinct().shuffled() }
    val slots = remember(q.id) { mutableStateListOf<String?>().apply { repeat(q.parts.size) { add(null) } } }

    fun publish() = onAnswer(if (slots.all { it != null }) Answer.Meanings(slots.map { it!! }) else null)

    Text("Wortbausteine", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Prompt("Was bedeuten die Bausteine von „${q.term}“?")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.parts.forEachIndexed { i, part ->
            val (bg, fg) = PartColors[i % PartColors.size]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(12.dp)) {
                    Text(part.part, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                }
                Spacer(Modifier.height(6.dp))
                val filled = slots[i]
                Surface(
                    onClick = { slots[i] = null; publish() },
                    enabled = !locked && filled != null,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(if (filled == null) 1.dp else 2.dp, if (filled == null) MaterialTheme.colorScheme.outline else fg),
                    color = if (filled == null) MaterialTheme.colorScheme.surface else bg,
                    modifier = Modifier.widthIn(min = 96.dp).heightIn(min = 44.dp),
                ) {
                    Text(filled ?: "?", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp))
                }
            }
        }
    }
    Text("Tippe eine Bedeutung an, um das nächste freie Feld zu füllen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bank.forEach { meaning ->
            val used = meaning in slots
            Surface(
                onClick = {
                    val free = slots.indexOfFirst { it == null }
                    if (free >= 0) { slots[free] = meaning; publish() }
                },
                enabled = !locked && !used,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                color = if (used) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                contentColor = if (used) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface,
            ) {
                Text(meaning, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
        }
    }
}

/** Kurzer Text der richtigen Lösung für das Feedback. */
fun correctAnswerText(q: Question): String = when (q) {
    is Question.SingleChoice -> q.options[q.correctIndex]
    is Question.TrueFalse -> if (q.correct) "Richtig" else "Falsch"
    is Question.Matching -> q.pairs.joinToString("\n") { "${it.left} → ${it.right}" }
    is Question.WordParts -> q.parts.joinToString("  |  ") { "${it.part} = ${it.meaning}" } + "\n${q.term}: ${q.meaning}"
}
