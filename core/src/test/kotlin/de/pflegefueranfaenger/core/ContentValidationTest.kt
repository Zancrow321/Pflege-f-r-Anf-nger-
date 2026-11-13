package de.pflegefueranfaenger.core

import de.pflegefueranfaenger.core.model.ArticleKind
import de.pflegefueranfaenger.core.model.ArticleTemplate
import de.pflegefueranfaenger.core.model.Level
import de.pflegefueranfaenger.core.model.Question
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Prüft die Inhaltsdateien auf formale Fehler. Fachliche Richtigkeit kann ein Test nicht prüfen,
 * aber er stellt sicher, dass jede Lektion, jeder Artikel und jeder Begriff Quellen hat
 * und alle Verweise auflösbar sind.
 */
class ContentValidationTest {
    private val c = TestContent.bundle

    @Test
    fun `alle IDs sind eindeutig`() {
        val ids = c.units.map { it.id } + c.lessons.map { it.id } + c.lessons.flatMap { l -> l.questions.map { it.id } } +
            c.articles.map { it.id } + c.terms.map { it.id } + c.sources.map { it.id }
        val dupes = ids.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(dupes.isEmpty(), "Doppelte IDs: $dupes")
    }

    @Test
    fun `jede Lektion, jeder Artikel und jeder Begriff hat gültige Quellen`() {
        val sourceIds = c.sources.map { it.id }.toSet()
        val problems = mutableListOf<String>()
        c.lessons.forEach { l ->
            if (l.sourceIds.isEmpty()) problems += "Lektion ${l.id} ohne Quelle"
            l.sourceIds.filter { it !in sourceIds }.forEach { problems += "Lektion ${l.id}: unbekannte Quelle $it" }
        }
        c.articles.forEach { a ->
            if (a.sourceIds.isEmpty()) problems += "Artikel ${a.id} ohne Quelle"
            a.sourceIds.filter { it !in sourceIds }.forEach { problems += "Artikel ${a.id}: unbekannte Quelle $it" }
            LocalDate.parse(a.lastChecked)
        }
        c.terms.forEach { t ->
            if (t.sourceIds.isEmpty()) problems += "Begriff ${t.id} ohne Quelle"
            t.sourceIds.filter { it !in sourceIds }.forEach { problems += "Begriff ${t.id}: unbekannte Quelle $it" }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `Verweise auf Artikel und Begriffe sind auflösbar`() {
        val problems = mutableListOf<String>()
        c.lessons.forEach { l ->
            l.articleIds.filter { c.article(it) == null }.forEach { problems += "Lektion ${l.id}: Artikel $it fehlt" }
            l.termIds.filter { c.term(it) == null }.forEach { problems += "Lektion ${l.id}: Begriff $it fehlt" }
        }
        c.articles.forEach { a ->
            a.termIds.filter { c.term(it) == null }.forEach { problems += "Artikel ${a.id}: Begriff $it fehlt" }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `Fragen sind wohlgeformt`() {
        val problems = mutableListOf<String>()
        c.lessons.flatMap { it.questions }.forEach { q ->
            if (q.explanation.isBlank()) problems += "${q.id}: Erklärung fehlt"
            when (q) {
                is Question.SingleChoice -> {
                    if (q.options.size !in 2..4) problems += "${q.id}: 2–4 Antwortoptionen erwartet"
                    if (q.correctIndex !in q.options.indices) problems += "${q.id}: correctIndex ungültig"
                    if (q.options.toSet().size != q.options.size) problems += "${q.id}: doppelte Optionen"
                }
                is Question.TrueFalse -> Unit
                is Question.Matching -> {
                    if (q.pairs.size !in 2..5) problems += "${q.id}: 2–5 Paare erwartet"
                    if (q.pairs.map { it.left }.toSet().size != q.pairs.size) problems += "${q.id}: linke Seite doppelt"
                    if (q.pairs.map { it.right }.toSet().size != q.pairs.size) problems += "${q.id}: rechte Seite doppelt"
                }
                is Question.WordParts -> {
                    if (q.parts.size < 2) problems += "${q.id}: mindestens 2 Bausteine"
                    val meanings = q.parts.map { it.meaning }
                    if (q.distractors.any { it in meanings }) problems += "${q.id}: Ablenker ist zugleich richtige Bedeutung"
                    val joined = q.parts.joinToString("") { it.part }
                    if (!joined.equals(q.term, ignoreCase = true)) problems += "${q.id}: Bausteine ergeben nicht den Begriff ($joined ≠ ${q.term})"
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `Bausteine der Begriffe ergeben den Begriff`() {
        val problems = c.terms.filter { t ->
            !t.parts.joinToString("") { it.part }.equals(t.term, ignoreCase = true)
        }.map { "${it.id}: ${it.parts.joinToString("|") { p -> p.part }} ≠ ${it.term}" }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `Einstiegstest nutzt nur Grundlagen-Fragen`() {
        val basicQuestionIds = c.units.filter { it.level == Level.GRUNDLAGEN }
            .flatMap { u -> u.lessons.flatMap { l -> l.questions.map { it.id } } }.toSet()
        assertEquals(c.placement.questionIds.size, c.placementQuestions().size, "Unbekannte Frage im Einstiegstest")
        c.placement.questionIds.forEach {
            if (it !in basicQuestionIds) fail("Einstiegstest-Frage $it gehört nicht zu den Grundlagen")
        }
        assertTrue(c.placement.passPercent in 50..100)
    }

    @Test
    fun `es gibt eine Fachsprache-Einheit mit Wortbaustein-Fragen`() {
        val wordParts = c.lessons.flatMap { it.questions }.filterIsInstance<Question.WordParts>()
        assertTrue(wordParts.size >= 10, "Mindestens 10 Wortbaustein-Fragen erwartet, gefunden: ${wordParts.size}")
        assertTrue(c.terms.size >= 30)
    }

    @Test
    fun `Wiki-Artikel folgen der Gliederung ihrer Art`() {
        val problems = mutableListOf<String>()
        c.articles.forEach { a ->
            val required = ArticleTemplate.requiredSections(a.kind)
            val titles = a.sections.map { it.title }
            if (a.kind == ArticleKind.WISSEN && a.body.isEmpty() && a.sections.isEmpty()) problems += "${a.id}: kein Inhalt"
            val missing = required.filter { it !in titles }
            if (missing.isNotEmpty()) problems += "${a.id} (${a.kind.label}): Abschnitte fehlen: $missing"
            // Reihenfolge der Pflichtabschnitte einhalten
            val order = titles.filter { it in required }
            if (order != required.filter { it in titles }) problems += "${a.id}: Abschnitte in falscher Reihenfolge"
            a.sections.filter { it.paragraphs.isEmpty() || it.paragraphs.any(String::isBlank) }
                .forEach { problems += "${a.id}: Abschnitt „${it.title}“ ist leer" }
            a.relatedArticleIds.filter { c.article(it) == null }.forEach { problems += "${a.id}: Querverweis $it fehlt" }
            if (a.id in a.relatedArticleIds) problems += "${a.id}: verweist auf sich selbst"
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }
}
