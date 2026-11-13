package de.pflegefueranfaenger.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Inhaltsmodell der App. Alle Inhalte liegen als JSON in /content und werden
 * unverändert als Android-Assets ausgeliefert. Das Modell ist so geschnitten,
 * dass es später 1:1 in Supabase-Tabellen übertragen werden kann
 * (units, lessons, questions, articles, terms, sources).
 */

/** Fachlicher Prüfstatus eines Inhalts. Aktuell ist alles UNREVIEWED („nicht fachlich geprüft“). */
@Serializable
enum class ReviewStatus {
    @SerialName("unreviewed") UNREVIEWED,
    @SerialName("reviewed") REVIEWED,
}

/** Niveau einer Einheit. GRUNDLAGEN kann über den Einstiegstest übersprungen werden. */
@Serializable
enum class Level {
    @SerialName("grundlagen") GRUNDLAGEN,
    @SerialName("station") STATION,
}

@Serializable
data class Source(
    val id: String,
    /** Zitat im Vancouver-Stil. */
    val citation: String,
    val url: String? = null,
    val year: Int? = null,
)

@Serializable
data class LearnCard(
    val title: String,
    val text: String,
    val bullets: List<String> = emptyList(),
    /** Optionaler EKG-Streifen, der auf der Lernkarte gezeichnet wird. */
    val strip: Rhythm? = null,
)

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val card: LearnCard,
    val questions: List<Question>,
    val sourceIds: List<String>,
    val articleIds: List<String> = emptyList(),
    val termIds: List<String> = emptyList(),
)

@Serializable
data class LearningUnit(
    val id: String,
    val title: String,
    val description: String,
    val level: Level,
    val lessons: List<Lesson>,
)

@Serializable
data class WordPart(
    /** Baustein, z.B. „Hypo“. */
    val part: String,
    /** Einzelbedeutung, z.B. „unter, zu wenig“. */
    val meaning: String,
    /** Herkunft, z.B. „griech. hypó“. */
    val origin: String? = null,
)

@Serializable
sealed interface Question {
    val id: String
    val explanation: String
    val strip: Rhythm?

    @Serializable
    @SerialName("single_choice")
    data class SingleChoice(
        override val id: String,
        val prompt: String,
        val options: List<String>,
        val correctIndex: Int,
        override val explanation: String,
        override val strip: Rhythm? = null,
    ) : Question

    @Serializable
    @SerialName("true_false")
    data class TrueFalse(
        override val id: String,
        val statement: String,
        val correct: Boolean,
        override val explanation: String,
        override val strip: Rhythm? = null,
    ) : Question

    @Serializable
    @SerialName("matching")
    data class Matching(
        override val id: String,
        val prompt: String,
        val pairs: List<Pair>,
        override val explanation: String,
        override val strip: Rhythm? = null,
    ) : Question {
        @Serializable
        data class Pair(val left: String, val right: String)
    }

    /**
     * Wortbausteine: Ein Fachbegriff wird in Bausteine zerlegt (z.B. Hypo | Thermie).
     * Für jeden Baustein wird die passende Einzelbedeutung gewählt.
     */
    @Serializable
    @SerialName("word_parts")
    data class WordParts(
        override val id: String,
        val term: String,
        val parts: List<WordPart>,
        /** Falsche Bedeutungen, die zusätzlich zur Auswahl stehen. */
        val distractors: List<String>,
        /** Gesamtbedeutung des Begriffs. */
        val meaning: String,
        override val explanation: String,
        override val strip: Rhythm? = null,
    ) : Question
}

/** Art eines Wiki-Artikels. Diagnosen, Prozeduren, Therapien und Medikamente folgen einer festen Gliederung. */
@Serializable
enum class ArticleKind(val label: String, val plural: String) {
    @SerialName("wissen") WISSEN("Wissen", "Wissen"),
    @SerialName("diagnose") DIAGNOSE("Diagnose", "Diagnosen"),
    @SerialName("prozedur") PROZEDUR("Prozedur", "Prozeduren"),
    @SerialName("therapie") THERAPIE("Therapie", "Therapien"),
    @SerialName("medikament") MEDIKAMENT("Medikament", "Medikamente"),
}

/** Abschnitt eines Wiki-Artikels. Absätze mit „- “ am Anfang werden als Aufzählung dargestellt. */
@Serializable
data class Section(val title: String, val paragraphs: List<String>)

/** Feste Gliederung je Artikelart, damit alle Wiki-Seiten gleich aufgebaut sind. */
object ArticleTemplate {
    val DIAGNOSE = listOf(
        "Kurz erklärt", "Ursachen und Risikofaktoren", "Symptome", "Diagnostik",
        "Therapie", "Pflege und Beobachtung", "Warnzeichen",
    )
    val PROZEDUR = listOf(
        "Kurz erklärt", "Indikationen", "Ablauf", "Vorbereitung", "Nachsorge und Pflege", "Komplikationen",
    )
    val THERAPIE = listOf(
        "Kurz erklärt", "Indikationen", "Ablauf", "Pflege und Beobachtung", "Risiken",
    )
    val MEDIKAMENT = listOf(
        "Wirkstoffe und Beispiele", "Wirkung", "Indikationen", "Nebenwirkungen",
        "Kontraindikationen und Vorsicht", "Pflege und Beobachtung",
    )

    fun requiredSections(kind: ArticleKind): List<String> = when (kind) {
        ArticleKind.WISSEN -> emptyList()
        ArticleKind.DIAGNOSE -> DIAGNOSE
        ArticleKind.PROZEDUR -> PROZEDUR
        ArticleKind.THERAPIE -> THERAPIE
        ArticleKind.MEDIKAMENT -> MEDIKAMENT
    }
}

@Serializable
data class Article(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    /** Absätze. Zeilen, die mit „- “ beginnen, werden als Aufzählung dargestellt. */
    val body: List<String> = emptyList(),
    val kind: ArticleKind = ArticleKind.WISSEN,
    /** Gegliederte Abschnitte (Pflicht für Diagnosen, Prozeduren, Therapien, Medikamente). */
    val sections: List<Section> = emptyList(),
    /** Querverweise auf andere Artikel („Siehe auch“). */
    val relatedArticleIds: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val sourceIds: List<String>,
    /** Datum der letzten Quellenprüfung (ISO, yyyy-MM-dd). */
    val lastChecked: String,
    /** Inhalt hängt von hausinternen Standards ab (SOP, Alarmgrenzen, Medikamente). */
    val houseSpecific: Boolean = false,
    val status: ReviewStatus = ReviewStatus.UNREVIEWED,
    val termIds: List<String> = emptyList(),
)

@Serializable
data class Term(
    val id: String,
    val term: String,
    val parts: List<WordPart>,
    val meaning: String,
    val example: String? = null,
    val sourceIds: List<String>,
    val status: ReviewStatus = ReviewStatus.UNREVIEWED,
)

@Serializable
data class PlacementTest(
    /** Ab diesem Anteil richtiger Antworten (0..100) gelten die Grundlagen als bestanden. */
    val passPercent: Int,
    val questionIds: List<String>,
)

@Serializable
data class UnitsFile(val units: List<LearningUnit>)

/** Alle Inhalte zusammen. */
data class ContentBundle(
    val units: List<LearningUnit>,
    val articles: List<Article>,
    val terms: List<Term>,
    val sources: List<Source>,
    val placement: PlacementTest,
) {
    val lessons: List<Lesson> by lazy { units.flatMap { it.lessons } }
    private val questionIndex by lazy { lessons.flatMap { it.questions }.associateBy { it.id } }
    private val lessonIndex by lazy { lessons.associateBy { it.id } }
    private val articleIndex by lazy { articles.associateBy { it.id } }
    private val termIndex by lazy { terms.associateBy { it.id } }
    private val sourceIndex by lazy { sources.associateBy { it.id } }
    private val unitOfLesson by lazy {
        units.flatMap { u -> u.lessons.map { it.id to u } }.toMap()
    }

    fun question(id: String): Question? = questionIndex[id]
    fun lesson(id: String): Lesson? = lessonIndex[id]
    fun article(id: String): Article? = articleIndex[id]
    fun term(id: String): Term? = termIndex[id]
    fun source(id: String): Source? = sourceIndex[id]
    fun unitOf(lessonId: String): LearningUnit? = unitOfLesson[lessonId]

    fun placementQuestions(): List<Question> = placement.questionIds.mapNotNull { question(it) }

    private val lessonsByArticle by lazy {
        lessons.flatMap { l -> l.articleIds.map { it to l } }.groupBy({ it.first }, { it.second })
    }
    private val referencingArticles by lazy {
        articles.flatMap { a -> a.relatedArticleIds.map { it to a } }.groupBy({ it.first }, { it.second })
    }

    /** Lektionen, die auf diesen Artikel verweisen („Dazu lernen“). */
    fun lessonsFor(articleId: String): List<Lesson> = lessonsByArticle[articleId].orEmpty()

    /** Querverweise in beide Richtungen, ohne Doppelte. */
    fun relatedTo(articleId: String): List<Article> {
        val own = article(articleId)?.relatedArticleIds.orEmpty().mapNotNull { article(it) }
        return (own + referencingArticles[articleId].orEmpty()).distinctBy { it.id }.filter { it.id != articleId }
    }
}
