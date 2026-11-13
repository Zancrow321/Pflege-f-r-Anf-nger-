package de.pflegefueranfaenger.core

import de.pflegefueranfaenger.core.game.Progress
import de.pflegefueranfaenger.core.game.ProgressRules
import de.pflegefueranfaenger.core.game.SessionResult
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressTest {
    private val day = LocalDate.of(2026, 11, 13)
    private val content = TestContent.bundle
    private val min = 60_000L

    @Test
    fun `Streak zählt aufeinanderfolgende Tage und reißt nach einer Lücke`() {
        var p = ProgressRules.addXp(Progress(), 10, day)
        assertEquals(1, p.streak)
        p = ProgressRules.addXp(p, 10, day) // gleicher Tag
        assertEquals(1, p.streak)
        assertEquals(20, p.xpToday)
        p = ProgressRules.addXp(p, 10, day.plusDays(1))
        assertEquals(2, p.streak)
        assertEquals(10, p.xpToday)
        assertEquals(2, ProgressRules.currentStreak(p, day.plusDays(2)))
        assertEquals(0, ProgressRules.currentStreak(p, day.plusDays(3)))
        p = ProgressRules.addXp(p, 10, day.plusDays(4))
        assertEquals(1, p.streak)
        assertEquals(2, p.longestStreak)
        assertEquals(40, p.xp)
    }

    @Test
    fun `Herzen füllen sich zeitbasiert wieder auf`() {
        val t0 = 1_000_000_000L
        var p = Progress()
        p = ProgressRules.loseHeart(p, t0)
        p = ProgressRules.loseHeart(p, t0 + 5 * min)
        assertEquals(3, ProgressRules.heartsAt(p, t0 + 10 * min))
        assertEquals(20, ProgressRules.minutesToNextHeart(p, t0 + 10 * min))
        assertEquals(4, ProgressRules.heartsAt(p, t0 + 30 * min))
        assertEquals(5, ProgressRules.heartsAt(p, t0 + 60 * min))
        assertNull(ProgressRules.minutesToNextHeart(p, t0 + 60 * min))
        val n = ProgressRules.normalizeHearts(p, t0 + 90 * min)
        assertEquals(5, n.hearts)
        assertNull(n.heartsRefillFrom)
    }

    @Test
    fun `Herzen werden nicht negativ`() {
        var p = Progress(hearts = 1, heartsRefillFrom = 0L)
        p = ProgressRules.loseHeart(p, 1)
        p = ProgressRules.loseHeart(p, 2)
        assertEquals(0, p.hearts)
    }

    @Test
    fun `Lektion abschließen gibt XP, Perfekt-Bonus und merkt Fehler`() {
        val perfect = SessionResult(setOf("a", "b"), emptySet(), passed = true)
        var p = ProgressRules.completeLesson(Progress(mistakes = setOf("a")), "u1l1", perfect, day)
        assertEquals(15, p.xp)
        assertTrue("u1l1" in p.completedLessons)
        assertTrue(p.mistakes.isEmpty())

        val withMistake = SessionResult(setOf("a"), setOf("b"), passed = true)
        p = ProgressRules.completeLesson(p, "u1l2", withMistake, day)
        assertEquals(25, p.xp)
        assertEquals(setOf("b"), p.mistakes)

        val failed = SessionResult(emptySet(), setOf("c"), passed = false)
        val q = ProgressRules.completeLesson(p, "u1l3", failed, day)
        assertFalse("u1l3" in q.completedLessons)
        assertEquals(25, q.xp)
        assertEquals(setOf("b", "c"), q.mistakes)
    }

    @Test
    fun `Wiederholung entfernt richtig beantwortete Fehler`() {
        val p = Progress(mistakes = setOf("a", "b"))
        val r = ProgressRules.completeReview(p, SessionResult(setOf("a"), setOf("b"), passed = true), day)
        assertEquals(setOf("b"), r.mistakes)
        assertEquals(Progress.XP_REVIEW, r.xp)
    }

    @Test
    fun `Lektionen werden der Reihe nach freigeschaltet`() {
        val ids = content.lessons.map { it.id }
        val p = Progress()
        assertTrue(ProgressRules.isUnlocked(p, content, ids[0]))
        assertFalse(ProgressRules.isUnlocked(p, content, ids[1]))
        val p2 = p.copy(completedLessons = setOf(ids[0]))
        assertTrue(ProgressRules.isUnlocked(p2, content, ids[1]))
        assertEquals(ids[1], ProgressRules.nextLesson(p2, content))
    }

    @Test
    fun `bestandener Einstiegstest überspringt alle Grundlagen`() {
        val qs = content.placement.questionIds
        val passed = SessionResult(qs.drop(1).toSet(), setOf(qs.first()), passed = true) // 90 %
        val p = ProgressRules.applyPlacement(Progress(), content, passed)
        assertEquals(true, p.placementPassed)
        val firstStation = content.units.first { it.level.name == "STATION" }.lessons.first().id
        assertEquals(firstStation, ProgressRules.nextLesson(p, content))

        val failed = SessionResult(qs.take(5).toSet(), qs.drop(5).toSet(), passed = true) // 50 %
        val f = ProgressRules.applyPlacement(Progress(), content, failed)
        assertEquals(false, f.placementPassed)
        assertTrue(f.completedLessons.isEmpty())
    }
}
