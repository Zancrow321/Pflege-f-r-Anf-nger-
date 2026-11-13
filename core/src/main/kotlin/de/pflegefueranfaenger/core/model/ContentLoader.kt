package de.pflegefueranfaenger.core.model

import kotlinx.serialization.json.Json

/** Liest die Inhaltsdateien. Plattformunabhängig: der Aufrufer liefert den Text der Dateien. */
object ContentLoader {
    const val UNITS = "units.json"
    const val ARTICLES = "articles.json"
    const val TERMS = "terms.json"
    const val SOURCES = "sources.json"
    const val PLACEMENT = "placement.json"

    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    fun load(read: (fileName: String) -> String): ContentBundle = ContentBundle(
        units = json.decodeFromString<UnitsFile>(read(UNITS)).units,
        articles = json.decodeFromString(read(ARTICLES)),
        terms = json.decodeFromString(read(TERMS)),
        sources = json.decodeFromString(read(SOURCES)),
        placement = json.decodeFromString(read(PLACEMENT)),
    )
}
