package de.pflegefueranfaenger.app.ui.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.pflegefueranfaenger.app.LoadedContent
import de.pflegefueranfaenger.app.ui.components.EcgStrip
import de.pflegefueranfaenger.app.ui.components.IllustrationView
import de.pflegefueranfaenger.app.ui.components.UnreviewedBadge
import de.pflegefueranfaenger.app.ui.theme.CorrectGreen
import de.pflegefueranfaenger.app.ui.theme.HeartRed
import de.pflegefueranfaenger.app.ui.theme.WrongRed
import de.pflegefueranfaenger.app.ui.theme.XpGold
import de.pflegefueranfaenger.core.game.Answer
import de.pflegefueranfaenger.core.game.Progress
import de.pflegefueranfaenger.core.game.SessionResult
import de.pflegefueranfaenger.core.model.LearnCard
import de.pflegefueranfaenger.core.model.Question

/**
 * Spielt eine Lektion, eine Wiederholung oder den Einstiegstest.
 * Ablauf bei Lektionen: Lernkarte → Fragen mit Feedback → Ergebnis.
 */
@Composable
fun LessonScreen(
    mode: SessionMode,
    content: LoadedContent,
    progress: Progress,
    heartsNow: Int,
    minutesToNextHeart: Long?,
    onLoseHeart: () -> Unit,
    onFinished: (SessionResult) -> Unit,
    onClose: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onStartReview: () -> Unit,
) {
    val bundle = content.bundle
    val lesson = (mode as? SessionMode.Lesson)?.let { bundle.lesson(it.lessonId) }
    val questions: List<Question> = remember(mode) {
        when (mode) {
            is SessionMode.Lesson -> lesson?.questions.orEmpty()
            SessionMode.Placement -> bundle.placementQuestions()
            SessionMode.Review -> progress.mistakes.mapNotNull { bundle.question(it) }.shuffled().take(10)
        }
    }
    if (questions.isEmpty()) {
        MessageScreen("Hier gibt es gerade nichts zu üben.", "Zurück", onClose)
        return
    }
    val usesHearts = mode is SessionMode.Lesson
    // Herzen nur beim Start prüfen: Geht das letzte Herz während der Lektion verloren, zeigt die Lektion selbst das Ergebnis.
    val startHearts = remember(mode) { heartsNow }
    if (usesHearts && startHearts <= 0) {
        NoHeartsScreen(minutesToNextHeart, hasMistakes = progress.mistakes.isNotEmpty(), onReview = onStartReview, onClose = onClose)
        return
    }

    val controller = remember(mode) {
        SessionController(
            questions = questions,
            requeueWrong = mode !is SessionMode.Placement,
            hearts = if (usesHearts) startHearts else null,
        )
    }
    var showCard by remember(mode) { mutableStateOf(lesson != null) }
    var result by remember(mode) { mutableStateOf<SessionResult?>(null) }

    LaunchedEffect(controller.finished) {
        if (controller.finished && result == null) {
            val r = controller.result()
            result = r
            onFinished(r)
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Schließen") }
            LinearProgressIndicator(
                progress = { controller.progress },
                modifier = Modifier.weight(1f).height(10.dp),
            )
            controller.heartsLeft?.let {
                Text("  ❤ $it", color = HeartRed, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.padding(end = 8.dp))
        }

        val r = result
        when {
            r != null -> ResultView(mode, r, onClose)
            showCard && lesson != null -> CardView(lesson.title, lesson.card, lesson.articleIds.mapNotNull { bundle.article(it) }.map { it.id to it.title },
                onOpenArticle, onStart = { showCard = false })
            else -> {
                val q = controller.shown ?: return@Column
                var pending by remember(controller.attempt) { mutableStateOf<Answer?>(null) }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    key(controller.attempt) {
                        QuestionView(q, locked = controller.feedback != null) { pending = it }
                    }
                }
                val fb = controller.feedback
                if (fb == null) {
                    Button(
                        onClick = {
                            val a = pending ?: return@Button
                            val ok = controller.submit(a)
                            if (!ok && usesHearts) onLoseHeart()
                        },
                        enabled = pending != null,
                        modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp),
                    ) { Text("Prüfen", style = MaterialTheme.typography.titleMedium) }
                } else {
                    FeedbackPanel(fb, onNext = { controller.next() })
                }
            }
        }
    }
}

@Composable
private fun CardView(title: String, card: LearnCard, articles: List<Pair<String, String>>, onOpenArticle: (String) -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(card.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            UnreviewedBadge()
            card.strip?.let { EcgStrip(it, showLabel = true) }
            card.illustration?.let { IllustrationView(it, interactive = true) }
            Text(card.text, style = MaterialTheme.typography.bodyLarge)
            card.bullets.forEach { b ->
                Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text(b, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (articles.isNotEmpty()) {
                Text("Mehr dazu im Nachschlagewerk", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                articles.forEach { (id, t) -> OutlinedButton(onClick = { onOpenArticle(id) }) { Text(t) } }
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(20.dp).height(52.dp)) {
            Text("Los geht's", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FeedbackPanel(fb: Feedback, onNext: () -> Unit) {
    val color = if (fb.correct) CorrectGreen else WrongRed
    Surface(color = color.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (fb.correct) "Richtig!" else "Nicht ganz.", color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!fb.correct) {
                Text("Richtige Antwort:", style = MaterialTheme.typography.labelLarge, color = color)
                Text(correctAnswerText(fb.question), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(fb.question.explanation, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Weiter", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun ResultView(mode: SessionMode, r: SessionResult, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        when (mode) {
            is SessionMode.Lesson -> if (r.passed) {
                Text(if (r.perfect) "🏆" else "🎉", style = MaterialTheme.typography.displayLarge)
                Text("Lektion geschafft!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                val xp = Progress.XP_LESSON + if (r.perfect) Progress.XP_PERFECT_BONUS else 0
                Text("+$xp XP", color = XpGold, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${r.percent} % beim ersten Versuch richtig" + if (r.perfect) " – fehlerfrei!" else "", textAlign = TextAlign.Center)
                if (!r.perfect) Text("Deine Fehler landen in der Wiederholung.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("💔", style = MaterialTheme.typography.displayLarge)
                Text("Keine Herzen mehr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Kein Problem: Die Herzen füllen sich mit der Zeit wieder auf. Deine Fehler kannst du in der Wiederholung üben.", textAlign = TextAlign.Center)
            }
            SessionMode.Review -> {
                Text("💪", style = MaterialTheme.typography.displayLarge)
                Text("Wiederholung beendet", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${r.correctQuestionIds.size} von ${r.total} Fehlern beim ersten Versuch richtig.", textAlign = TextAlign.Center)
            }
            SessionMode.Placement -> {
                Text("📋", style = MaterialTheme.typography.displayLarge)
                Text("Einstiegstest: ${r.percent} %", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Wenn du bestanden hast, sind die Grundlagen-Einheiten freigeschaltet und als erledigt markiert. Du kannst sie trotzdem jederzeit wiederholen. Sonst startest du bei den Grundlagen.",
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Weiter") }
    }
}

@Composable
private fun NoHeartsScreen(minutes: Long?, hasMistakes: Boolean, onReview: () -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("💔", style = MaterialTheme.typography.displayLarge)
        Text("Keine Herzen mehr", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            (minutes?.let { "Das nächste Herz kommt in $it Minuten. " } ?: "") +
                "Bis dahin kannst du deine Fehler wiederholen oder im Nachschlagewerk lesen.",
            textAlign = TextAlign.Center,
        )
        if (hasMistakes) Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) { Text("Fehler wiederholen") }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Zurück") }
    }
}

@Composable
private fun MessageScreen(text: String, button: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text, textAlign = TextAlign.Center)
            Button(onClick = onClick) { Text(button) }
        }
    }
}
