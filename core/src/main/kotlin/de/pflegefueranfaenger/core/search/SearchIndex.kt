package de.pflegefueranfaenger.core.search

import de.pflegefueranfaenger.core.model.Article
import de.pflegefueranfaenger.core.model.ContentBundle
import de.pflegefueranfaenger.core.model.Term
import java.text.Normalizer

/** Ein Treffer im Nachschlagewerk. */
sealed interface SearchHit {
    val score: Int
    val title: String

    data class ArticleHit(val article: Article, override val score: Int) : SearchHit {
        override val title get() = article.title
    }

    data class TermHit(val term: Term, override val score: Int) : SearchHit {
        override val title get() = term.term
    }
}

/**
 * Einfache, schnelle Volltextsuche im Speicher (der Inhalt ist klein).
 * Ignoriert Groß-/Kleinschreibung und Umlaut-Schreibweisen (ä = ae = a),
 * findet Wortanfänge und Teilwörter („kalium“ findet „Hypokaliämie“).
 */
class SearchIndex(private val content: ContentBundle) {

    private data class Doc(val hit: (Int) -> SearchHit, val title: String, val strong: String, val weak: String)

    private val docs: List<Doc> = content.articles.map { a ->
        Doc(
            hit = { s -> SearchHit.ArticleHit(a, s) },
            title = normalize(a.title),
            strong = normalize((a.keywords + a.category + a.kind.label).joinToString(" ")),
            weak = normalize(
                (listOf(a.summary) + a.body + a.sections.flatMap { listOf(it.title) + it.paragraphs }).joinToString(" "),
            ),
        )
    } + content.terms.map { t ->
        Doc(
            hit = { s -> SearchHit.TermHit(t, s) },
            title = normalize(t.term),
            strong = normalize(t.parts.joinToString(" ") { "${it.part} ${it.meaning}" }),
            weak = normalize(listOfNotNull(t.meaning, t.example).joinToString(" ")),
        )
    }

    fun search(query: String, limit: Int = 50): List<SearchHit> {
        val tokens = normalize(query).split(' ').filter { it.length >= 2 }
        if (tokens.isEmpty()) return emptyList()
        return docs.mapNotNull { d ->
            var total = 0
            for (tok in tokens) {
                val s = scoreToken(d, tok)
                if (s == 0) return@mapNotNull null // alle Suchwörter müssen vorkommen
                total += s
            }
            d.hit(total)
        }.sortedWith(compareByDescending<SearchHit> { it.score }.thenBy { it.title })
            .take(limit)
    }

    private fun scoreToken(d: Doc, tok: String): Int = when {
        d.title == tok -> 100
        d.title.startsWith(tok) -> 60
        d.title.split(' ').any { it.startsWith(tok) } -> 50
        d.title.contains(tok) -> 40
        d.strong.split(' ').any { it.startsWith(tok) } -> 25
        d.strong.contains(tok) -> 15
        d.weak.split(' ').any { it.startsWith(tok) } -> 8
        d.weak.contains(tok) -> 4
        else -> 0
    }

    companion object {
        /** Kleinbuchstaben, ä/ö/ü/ß vereinheitlicht, Satzzeichen entfernt. */
        fun normalize(text: String): String {
            val lower = text.lowercase()
                .replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss")
                .replace("ae", "a").replace("oe", "o").replace("ue", "u")
            val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replace(Regex("\\p{M}+"), "")
            return noAccents.replace(Regex("[^a-z0-9]+"), " ").trim()
        }
    }
}
