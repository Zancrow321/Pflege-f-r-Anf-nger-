package de.pflegefueranfaenger.core.game

import de.pflegefueranfaenger.core.model.ContentBundle
import de.pflegefueranfaenger.core.model.Level
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Lernfortschritt, lokal gespeichert (offline zuerst). Rein datenbasiert und
 * serialisierbar, damit er später mit Supabase synchronisiert werden kann.
 */
@Serializable
data class Progress(
    val xp: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    /** Letzter Tag mit abgeschlossener Lektion (ISO yyyy-MM-dd). */
    val lastActiveDay: String? = null,
    val xpToday: Int = 0,
    val dailyGoalXp: Int = 20,
    val hearts: Int = MAX_HEARTS,
    /** Zeitpunkt (epoch millis), ab dem die Herzen wieder aufgefüllt werden. */
    val heartsRefillFrom: Long? = null,
    val completedLessons: Set<String> = emptySet(),
    /** Fragen, die falsch beantwortet wurden und wiederholt werden sollen. */
    val mistakes: Set<String> = emptySet(),
    /** null = Einstieg noch nicht gewählt, true = Test bestanden, false = von vorne. */
    val placementPassed: Boolean? = null,
    val disclaimerAccepted: Boolean = false,
) {
    companion object {
        const val MAX_HEARTS = 5
        const val HEART_REFILL_MINUTES = 30L
        const val XP_LESSON = 10
        const val XP_PERFECT_BONUS = 5
        const val XP_REVIEW = 5
    }
}

/** Reine Funktionen auf dem Fortschritt. Zeit wird immer übergeben, damit alles testbar bleibt. */
object ProgressRules {

    private const val PERIOD_MILLIS = Progress.HEART_REFILL_MINUTES * 60_000

    /** Rechnet zeitbasiert aufgefüllte Herzen ein. */
    fun normalizeHearts(p: Progress, nowMillis: Long): Progress {
        val from = p.heartsRefillFrom ?: return p
        val refilled = ((nowMillis - from) / PERIOD_MILLIS).coerceAtLeast(0)
        val hearts = (p.hearts + refilled).coerceAtMost(Progress.MAX_HEARTS.toLong()).toInt()
        return if (hearts >= Progress.MAX_HEARTS) {
            p.copy(hearts = Progress.MAX_HEARTS, heartsRefillFrom = null)
        } else {
            p.copy(hearts = hearts, heartsRefillFrom = from + refilled * PERIOD_MILLIS)
        }
    }

    /** Herzen, die zum Zeitpunkt [nowMillis] verfügbar sind. */
    fun heartsAt(p: Progress, nowMillis: Long): Int = normalizeHearts(p, nowMillis).hearts

    /** Minuten bis zum nächsten Herz, oder null wenn voll. */
    fun minutesToNextHeart(p: Progress, nowMillis: Long): Long? {
        val n = normalizeHearts(p, nowMillis)
        val from = n.heartsRefillFrom ?: return null
        val remaining = from + PERIOD_MILLIS - nowMillis
        return (remaining + 59_999) / 60_000
    }

    /** Ein Herz abziehen. */
    fun loseHeart(p: Progress, nowMillis: Long): Progress {
        val n = normalizeHearts(p, nowMillis)
        if (n.hearts <= 0) return n
        return n.copy(
            hearts = n.hearts - 1,
            heartsRefillFrom = n.heartsRefillFrom ?: nowMillis,
        )
    }

    /** Streak-Wert, wie er heute angezeigt wird (0, wenn gestern nichts gelernt wurde). */
    fun currentStreak(p: Progress, today: LocalDate): Int {
        val last = p.lastActiveDay?.let(LocalDate::parse) ?: return 0
        val gap = ChronoUnit.DAYS.between(last, today)
        return if (gap <= 1) p.streak else 0
    }

    fun xpToday(p: Progress, today: LocalDate): Int =
        if (p.lastActiveDay == today.toString()) p.xpToday else 0

    /** XP gutschreiben und Streak fortschreiben. */
    fun addXp(p: Progress, xp: Int, today: LocalDate): Progress {
        val last = p.lastActiveDay?.let(LocalDate::parse)
        val gap = last?.let { ChronoUnit.DAYS.between(it, today) }
        val streak = when (gap) {
            null -> 1
            0L -> p.streak.coerceAtLeast(1)
            1L -> p.streak + 1
            else -> 1
        }
        return p.copy(
            xp = p.xp + xp,
            xpToday = (if (gap == 0L) p.xpToday else 0) + xp,
            streak = streak,
            longestStreak = maxOf(p.longestStreak, streak),
            lastActiveDay = today.toString(),
        )
    }

    /** Ergebnis einer Lektion übernehmen. */
    fun completeLesson(
        p: Progress,
        lessonId: String,
        result: SessionResult,
        today: LocalDate,
    ): Progress {
        if (!result.passed) {
            return p.copy(mistakes = p.mistakes + result.wrongQuestionIds)
        }
        val xp = Progress.XP_LESSON + if (result.perfect) Progress.XP_PERFECT_BONUS else 0
        val withXp = addXp(p, xp, today)
        return withXp.copy(
            completedLessons = p.completedLessons + lessonId,
            mistakes = p.mistakes - result.correctQuestionIds + result.wrongQuestionIds,
        )
    }

    /** Ergebnis einer Wiederholungsrunde übernehmen: richtig beantwortete Fehler werden entfernt. */
    fun completeReview(p: Progress, result: SessionResult, today: LocalDate): Progress {
        val cleaned = p.copy(mistakes = p.mistakes - result.correctQuestionIds)
        return if (result.correctQuestionIds.isEmpty()) cleaned else addXp(cleaned, Progress.XP_REVIEW, today)
    }

    /** Einstiegstest auswerten. Bestanden → alle Grundlagen-Lektionen gelten als abgeschlossen. */
    fun applyPlacement(p: Progress, content: ContentBundle, result: SessionResult): Progress {
        val total = result.correctQuestionIds.size + result.wrongQuestionIds.size
        val percent = if (total == 0) 0 else result.correctQuestionIds.size * 100 / total
        val passed = percent >= content.placement.passPercent
        if (!passed) return p.copy(placementPassed = false)
        val basics = content.units.filter { it.level == Level.GRUNDLAGEN }
            .flatMap { u -> u.lessons.map { it.id } }
        return p.copy(placementPassed = true, completedLessons = p.completedLessons + basics)
    }

    /** Lektionen sind der Reihe nach freigeschaltet: die erste offene Lektion ist spielbar. */
    fun isUnlocked(p: Progress, content: ContentBundle, lessonId: String): Boolean {
        val order = content.lessons.map { it.id }
        val idx = order.indexOf(lessonId)
        if (idx <= 0) return idx == 0
        return order[idx - 1] in p.completedLessons || lessonId in p.completedLessons
    }

    /** Nächste noch nicht abgeschlossene, freigeschaltete Lektion. */
    fun nextLesson(p: Progress, content: ContentBundle): String? =
        content.lessons.firstOrNull { it.id !in p.completedLessons && isUnlocked(p, content, it.id) }?.id
}
