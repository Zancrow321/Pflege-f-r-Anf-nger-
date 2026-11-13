package de.pflegefueranfaenger.core.game

import de.pflegefueranfaenger.core.model.Question

/** Antwort der Lernenden auf eine Frage. */
sealed interface Answer {
    data class Choice(val index: Int) : Answer
    data class Bool(val value: Boolean) : Answer
    /** Zuordnung linke Seite → gewählte rechte Seite. */
    data class Matches(val pairs: Map<String, String>) : Answer
    /** Gewählte Bedeutungen, in der Reihenfolge der Wortbausteine. */
    data class Meanings(val meanings: List<String>) : Answer
}

object AnswerChecker {
    fun isCorrect(question: Question, answer: Answer): Boolean = when (question) {
        is Question.SingleChoice -> answer is Answer.Choice && answer.index == question.correctIndex
        is Question.TrueFalse -> answer is Answer.Bool && answer.value == question.correct
        is Question.Matching -> answer is Answer.Matches &&
            question.pairs.size == answer.pairs.size &&
            question.pairs.all { answer.pairs[it.left] == it.right }
        is Question.WordParts -> answer is Answer.Meanings &&
            answer.meanings == question.parts.map { it.meaning }
    }
}

data class SessionResult(
    /** Beim ersten Versuch richtig beantwortet. */
    val correctQuestionIds: Set<String>,
    /** Beim ersten Versuch falsch beantwortet. */
    val wrongQuestionIds: Set<String>,
    /** Alle Fragen geschafft (bei Lektionen: ohne dass die Herzen ausgingen). */
    val passed: Boolean,
) {
    val perfect: Boolean get() = passed && wrongQuestionIds.isEmpty()
    val total: Int get() = correctQuestionIds.size + wrongQuestionIds.size
    val percent: Int get() = if (total == 0) 0 else correctQuestionIds.size * 100 / total
}

/**
 * Ablauf einer Lern-, Wiederholungs- oder Testrunde.
 *
 * - [requeueWrong]: falsch beantwortete Fragen kommen am Ende der Runde noch einmal (wie bei Lektionen).
 * - [hearts]: Anzahl verfügbarer Herzen, oder null wenn ohne Herzen gespielt wird (Wiederholung, Einstiegstest).
 */
class LessonSession(
    questions: List<Question>,
    private val requeueWrong: Boolean = true,
    hearts: Int? = null,
) {
    init {
        require(questions.isNotEmpty()) { "Eine Runde braucht mindestens eine Frage" }
    }

    private val queue = ArrayDeque(questions)
    private val requeued = mutableSetOf<String>()
    private val firstCorrect = linkedSetOf<String>()
    private val firstWrong = linkedSetOf<String>()

    var heartsLeft: Int? = hearts
        private set

    /** Anzahl Fragen der Runde ohne Wiederholungen, für den Fortschrittsbalken. */
    val plannedCount: Int = questions.size

    val current: Question? get() = if (isOver) null else queue.firstOrNull()

    val isOver: Boolean get() = queue.isEmpty() || heartsLeft == 0

    /** Fortschritt 0..1 für den Balken. */
    val progress: Float
        get() {
            val total = plannedCount + requeued.size
            val done = total - queue.size
            return if (total == 0) 1f else done.toFloat() / total
        }

    /** Beantwortet die aktuelle Frage und liefert, ob die Antwort richtig war. */
    fun answer(answer: Answer): Boolean {
        val q = checkNotNull(current) { "Runde ist bereits beendet" }
        val correct = AnswerChecker.isCorrect(q, answer)
        val firstAttempt = q.id !in firstCorrect && q.id !in firstWrong
        if (firstAttempt) {
            if (correct) firstCorrect += q.id else firstWrong += q.id
        }
        queue.removeFirst()
        if (!correct) {
            heartsLeft = heartsLeft?.let { (it - 1).coerceAtLeast(0) }
            if (requeueWrong && q.id !in requeued) {
                requeued += q.id
                queue.addLast(q)
            }
        }
        return correct
    }

    fun result(): SessionResult = SessionResult(
        correctQuestionIds = firstCorrect.toSet(),
        wrongQuestionIds = firstWrong.toSet(),
        passed = queue.isEmpty() && heartsLeft != 0,
    )
}
