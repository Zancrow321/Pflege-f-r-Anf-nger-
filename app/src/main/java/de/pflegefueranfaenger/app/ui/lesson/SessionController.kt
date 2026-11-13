package de.pflegefueranfaenger.app.ui.lesson

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.pflegefueranfaenger.core.game.Answer
import de.pflegefueranfaenger.core.game.LessonSession
import de.pflegefueranfaenger.core.game.SessionResult
import de.pflegefueranfaenger.core.model.Question

/** Welche Art von Runde gespielt wird. */
sealed interface SessionMode {
    data class Lesson(val lessonId: String) : SessionMode
    data object Placement : SessionMode
    data object Review : SessionMode
}

data class Feedback(val question: Question, val correct: Boolean)

/**
 * Compose-Zustand einer Runde. Die eigentliche Logik steckt in [LessonSession] (Modul :core).
 * Die beantwortete Frage bleibt mit Feedback sichtbar, bis „Weiter“ gedrückt wird.
 */
class SessionController(questions: List<Question>, requeueWrong: Boolean, hearts: Int?) {
    private val session = LessonSession(questions, requeueWrong, hearts)

    var shown: Question? by mutableStateOf(session.current)
        private set
    /** Zählt jede angezeigte Frage hoch, damit die Eingaben bei einer Wiederholung zurückgesetzt werden. */
    var attempt by mutableIntStateOf(0)
        private set
    var feedback: Feedback? by mutableStateOf(null)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var heartsLeft: Int? by mutableStateOf(hearts)
        private set
    var finished by mutableStateOf(false)
        private set

    fun submit(answer: Answer): Boolean {
        val q = shown ?: return false
        val ok = session.answer(answer)
        feedback = Feedback(q, ok)
        heartsLeft = session.heartsLeft
        progress = session.progress
        return ok
    }

    fun next() {
        feedback = null
        if (session.isOver) {
            finished = true
        } else {
            shown = session.current
            attempt++
        }
    }

    fun result(): SessionResult = session.result()
}
