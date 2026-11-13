package de.pflegefueranfaenger.core

import de.pflegefueranfaenger.core.ecg.EcgGenerator
import de.pflegefueranfaenger.core.model.Rhythm
import de.pflegefueranfaenger.core.search.SearchHit
import de.pflegefueranfaenger.core.search.SearchIndex
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchAndEcgTest {
    private val index = SearchIndex(TestContent.bundle)

    @Test
    fun `Suche findet Begriffe unabhängig von Umlaut-Schreibweise`() {
        val a = index.search("Hypokaliämie").first()
        val b = index.search("hypokaliaemie").first()
        assertEquals(a.title, b.title)
        assertTrue(index.search("hypokaliaemie").any { it is SearchHit.TermHit && it.term.id == "t-hypokaliaemie" })
    }

    @Test
    fun `Suche nach Baustein-Bedeutung findet Begriffe`() {
        val hits = index.search("Wärme").map { it.title }
        assertTrue("Hypothermie" in hits, "Treffer: $hits")
    }

    @Test
    fun `Suche nach Stichwort findet Artikel`() {
        val hits = index.search("Elektroden").map { it.title }
        assertTrue(hits.first().startsWith("Elektroden"), "Treffer: $hits")
        assertTrue(index.search("Kalium T-Welle").any { it is SearchHit.ArticleHit })
        assertTrue(index.search("x").isEmpty())
        assertTrue(index.search("gibtesnicht").isEmpty())
    }

    @Test
    fun `alle Rhythmen lassen sich erzeugen`() {
        Rhythm.entries.forEach { r ->
            val s = EcgGenerator.generate(r)
            assertEquals(6 * EcgGenerator.SAMPLE_RATE, s.size)
            assertTrue(s.all { it.isFinite() && abs(it) < 3f }, "Werte außerhalb des Bereichs bei $r")
        }
    }

    @Test
    fun `Asystolie ist flach, Sinus hat R-Zacken`() {
        val asys = EcgGenerator.generate(Rhythm.ASYSTOLE)
        assertTrue(asys.maxOf { abs(it) } < 0.1f)
        val sinus = EcgGenerator.generate(Rhythm.SINUS)
        // Anzahl R-Zacken (> 0,7 mV, lokales Maximum) in 6 s bei 75/min ≈ 7–8
        val peaks = (1 until sinus.size - 1).count { sinus[it] > 0.7f && sinus[it] >= sinus[it - 1] && sinus[it] > sinus[it + 1] }
        assertTrue(peaks in 7..8, "R-Zacken: $peaks")
    }
}
