package de.pflegefueranfaenger.core

import de.pflegefueranfaenger.core.game.Answer
import de.pflegefueranfaenger.core.game.AnswerChecker
import de.pflegefueranfaenger.core.game.LessonSession
import de.pflegefueranfaenger.core.model.Question
import de.pflegefueranfaenger.core.model.WordPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LessonSessionTest {
    private val q1 = Question.TrueFalse("q1", "A", correct = true, explanation = "e")
    private val q2 = Question.SingleChoice("q2", "B", listOf("x", "y"), correctIndex = 1, explanation = "e")
    private val hypothermie = Question.WordParts(
        id = "q3", term = "Hypothermie",
        parts = listOf(WordPart("Hypo", "unter, zu wenig"), WordPart("thermie", "Wärme")),
        distractors = listOf("Herz"), meaning = "Unterkühlung", explanation = "e",
    )

    @Test
    fun `Wortbausteine werden in der richtigen Reihenfolge geprüft`() {
        assertTrue(AnswerChecker.isCorrect(hypothermie, Answer.Meanings(listOf("unter, zu wenig", "Wärme"))))
        assertFalse(AnswerChecker.isCorrect(hypothermie, Answer.Meanings(listOf("Wärme", "unter, zu wenig"))))
        assertFalse(AnswerChecker.isCorrect(hypothermie, Answer.Meanings(listOf("unter, zu wenig", "Herz"))))
    }

    @Test
    fun `Zuordnung muss vollständig richtig sein`() {
        val m = Question.Matching("m", "p", listOf(Question.Matching.Pair("a", "1"), Question.Matching.Pair("b", "2")), "e")
        assertTrue(AnswerChecker.isCorrect(m, Answer.Matches(mapOf("a" to "1", "b" to "2"))))
        assertFalse(AnswerChecker.isCorrect(m, Answer.Matches(mapOf("a" to "2", "b" to "1"))))
        assertFalse(AnswerChecker.isCorrect(m, Answer.Matches(mapOf("a" to "1"))))
    }

    @Test
    fun `perfekte Runde`() {
        val s = LessonSession(listOf(q1, q2), hearts = 5)
        assertTrue(s.answer(Answer.Bool(true)))
        assertTrue(s.answer(Answer.Choice(1)))
        assertTrue(s.isOver)
        assertNull(s.current)
        val r = s.result()
        assertTrue(r.passed)
        assertTrue(r.perfect)
        assertEquals(100, r.percent)
        assertEquals(5, s.heartsLeft)
    }

    @Test
    fun `falsche Antwort kostet ein Herz und kommt am Ende nochmal`() {
        val s = LessonSession(listOf(q1, q2), hearts = 5)
        assertFalse(s.answer(Answer.Bool(false)))
        assertEquals(4, s.heartsLeft)
        assertEquals("q2", s.current?.id)
        s.answer(Answer.Choice(1))
        assertEquals("q1", s.current?.id) // Wiederholung
        s.answer(Answer.Bool(true))
        val r = s.result()
        assertTrue(r.passed)
        assertFalse(r.perfect)
        assertEquals(setOf("q1"), r.wrongQuestionIds)
        assertEquals(setOf("q2"), r.correctQuestionIds)
    }

    @Test
    fun `ohne Herzen ist die Runde verloren`() {
        val s = LessonSession(listOf(q1, q2), hearts = 1)
        s.answer(Answer.Bool(false))
        assertTrue(s.isOver)
        assertFalse(s.result().passed)
    }

    @Test
    fun `Einstiegstest wiederholt keine Fragen und kennt keine Herzen`() {
        val s = LessonSession(listOf(q1, q2), requeueWrong = false, hearts = null)
        s.answer(Answer.Bool(false))
        s.answer(Answer.Choice(0))
        assertTrue(s.isOver)
        val r = s.result()
        assertTrue(r.passed)
        assertEquals(0, r.percent)
    }
}
