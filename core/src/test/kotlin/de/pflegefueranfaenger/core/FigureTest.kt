package de.pflegefueranfaenger.core

import de.pflegefueranfaenger.core.figure.Element
import de.pflegefueranfaenger.core.figure.Figures
import de.pflegefueranfaenger.core.figure.Palette
import de.pflegefueranfaenger.core.figure.SvgExport
import de.pflegefueranfaenger.core.figure.Tone
import de.pflegefueranfaenger.core.model.Figure
import de.pflegefueranfaenger.core.model.Illustration
import de.pflegefueranfaenger.core.model.Question
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FigureTest {
    private val c = TestContent.bundle

    @Test
    fun `jedes Schaubild lässt sich in jeder Variante zeichnen`() {
        val problems = mutableListOf<String>()
        Figure.entries.forEach { f ->
            val variants = listOf(null) + Figures.parts(f).map { it.key }
            variants.forEach { h ->
                listOf(true, false).forEach { labels ->
                    val d = Figures.draw(f, h, labels)
                    if (d.elements.isEmpty()) problems += "$f/$h: leer"
                    d.elements.filterIsInstance<Element.TextEl>().forEach { t ->
                        if (t.x < 0 || t.x > d.width || t.y < 0 || t.y > d.height) problems += "$f/$h: Text „${t.text}“ außerhalb"
                    }
                    d.elements.filterIsInstance<Element.CircleEl>().forEach { e ->
                        if (e.cx - e.r < -1 || e.cx + e.r > d.width + 1 || e.cy - e.r < -1 || e.cy + e.r > d.height + 1) problems += "$f/$h: Kreis außerhalb"
                    }
                    if (h != null) {
                        val marked = d.elements.any {
                            when (it) {
                                is Element.PathEl -> it.fill == Tone.HIGHLIGHT || it.stroke == Tone.HIGHLIGHT
                                is Element.LineEl -> it.tone == Tone.HIGHLIGHT
                                is Element.CircleEl -> it.fill == Tone.HIGHLIGHT || it.stroke == Tone.HIGHLIGHT
                                is Element.TextEl -> it.tone == Tone.HIGHLIGHT
                            }
                        }
                        if (!marked) problems += "$f/$h: nichts hervorgehoben"
                    }
                    if (!labels && h != null && d.elements.none { it is Element.TextEl && it.text == "?" }) problems += "$f/$h: kein ?"
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `Schaubilder in den Inhalten sind gültig`() {
        val all = mutableListOf<Pair<String, Illustration>>()
        c.lessons.forEach { l ->
            l.card.illustration?.let { all += "Lektion ${l.id}" to it }
            l.questions.forEach { q -> q.illustration?.let { all += "Frage ${q.id}" to it } }
        }
        c.articles.forEach { a -> a.illustrations.forEach { all += "Artikel ${a.id}" to it } }
        c.terms.forEach { t -> t.illustration?.let { all += "Begriff ${t.id}" to it } }
        val problems = all.filter { (_, ill) ->
            ill.highlight != null && Figures.parts(ill.figure).none { it.key == ill.highlight }
        }.map { (where, ill) -> "$where: unbekannter Teil ${ill.highlight} in ${ill.figure}" }.toMutableList()
        all.filter { (_, ill) -> !ill.labels && ill.highlight == null }.forEach { (where, _) -> problems += "$where: ohne Beschriftung braucht es eine Markierung" }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
        assertTrue(all.size >= 20, "Mindestens 20 Schaubilder in den Inhalten erwartet, gefunden: ${all.size}")
    }

    @Test
    fun `Bildfragen verraten die Antwort nicht`() {
        val problems = mutableListOf<String>()
        c.lessons.flatMap { it.questions }.filterIsInstance<Question.SingleChoice>().forEach { q ->
            val ill = q.illustration ?: return@forEach
            if (ill.labels) return@forEach
            val texts = Figures.draw(ill).elements.filterIsInstance<Element.TextEl>().map { it.text.lowercase() }
            val answer = q.options[q.correctIndex].lowercase()
            if (texts.any { it.isNotBlank() && it != "?" && answer.startsWith(it) && it.length > 2 }) problems += "${q.id}: Bild zeigt die Antwort"
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `Vorschaubilder schreiben`() {
        val dir = File("build/figures").apply { mkdirs() }
        Figure.entries.forEach { f ->
            File(dir, "${f.name.lowercase()}.svg").writeText(SvgExport.toSvg(Figures.draw(f)))
            File(dir, "${f.name.lowercase()}_dark.svg").writeText(SvgExport.toSvg(Figures.draw(f), Palette.dark))
            Figures.parts(f).forEach { p ->
                File(dir, "${f.name.lowercase()}__${p.key}.svg").writeText(SvgExport.toSvg(Figures.draw(f, p.key, labels = true)))
                File(dir, "${f.name.lowercase()}__${p.key}_q.svg").writeText(SvgExport.toSvg(Figures.draw(f, p.key, labels = false)))
            }
        }
    }
}
