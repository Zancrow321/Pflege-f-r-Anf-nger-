package de.pflegefueranfaenger.app.data

import android.content.Context
import de.pflegefueranfaenger.core.model.ContentBundle
import de.pflegefueranfaenger.core.model.ContentLoader

/** Lädt die Lerninhalte aus den App-Assets (Quelle: Ordner /content im Repository). */
object ContentRepository {
    fun load(context: Context): ContentBundle = ContentLoader.load { fileName ->
        context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
