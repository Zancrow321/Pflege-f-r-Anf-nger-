package de.pflegefueranfaenger.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.pflegefueranfaenger.core.game.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.progressStore by preferencesDataStore(name = "progress")

/**
 * Speichert den Lernfortschritt lokal auf dem Gerät (offline zuerst).
 * Der Fortschritt wird als JSON abgelegt, damit er später unverändert mit Supabase synchronisiert werden kann.
 */
class ProgressRepository(private val context: Context) {
    private val key = stringPreferencesKey("progress_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val progress: Flow<Progress> = context.progressStore.data.map { prefs -> decode(prefs[key]) }

    suspend fun update(transform: (Progress) -> Progress) {
        context.progressStore.edit { prefs ->
            prefs[key] = json.encodeToString(Progress.serializer(), transform(decode(prefs[key])))
        }
    }

    private fun decode(raw: String?): Progress =
        raw?.let { runCatching { json.decodeFromString(Progress.serializer(), it) }.getOrNull() } ?: Progress()
}
