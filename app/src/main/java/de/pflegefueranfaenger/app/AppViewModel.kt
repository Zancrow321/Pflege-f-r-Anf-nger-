package de.pflegefueranfaenger.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.pflegefueranfaenger.app.data.ContentRepository
import de.pflegefueranfaenger.app.data.ProgressRepository
import de.pflegefueranfaenger.core.game.Progress
import de.pflegefueranfaenger.core.game.ProgressRules
import de.pflegefueranfaenger.core.game.SessionResult
import de.pflegefueranfaenger.core.model.ContentBundle
import de.pflegefueranfaenger.core.search.SearchIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Geladene Inhalte plus Suchindex. */
class LoadedContent(val bundle: ContentBundle) {
    val search = SearchIndex(bundle)
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val progressRepo = ProgressRepository(app)

    private val _content = MutableStateFlow<LoadedContent?>(null)
    val content: StateFlow<LoadedContent?> = _content.asStateFlow()

    val progress: StateFlow<Progress?> =
        progressRepo.progress.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Aktuelle Uhrzeit, alle 30 s aktualisiert (für das Auffüllen der Herzen). */
    val now: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    init {
        viewModelScope.launch {
            _content.value = withContext(Dispatchers.IO) { LoadedContent(ContentRepository.load(getApplication())) }
        }
    }

    private fun update(transform: (Progress) -> Progress) {
        viewModelScope.launch { progressRepo.update(transform) }
    }

    private fun today(): LocalDate = LocalDate.now()

    fun acceptDisclaimer() = update { it.copy(disclaimerAccepted = true) }

    fun startFromScratch() = update { it.copy(placementPassed = false) }

    fun loseHeart() = update { ProgressRules.loseHeart(it, System.currentTimeMillis()) }

    fun finishLesson(lessonId: String, result: SessionResult) =
        update { ProgressRules.completeLesson(it, lessonId, result, today()) }

    fun finishReview(result: SessionResult) = update { ProgressRules.completeReview(it, result, today()) }

    fun finishPlacement(result: SessionResult) {
        val bundle = _content.value?.bundle ?: return
        update { ProgressRules.applyPlacement(it, bundle, result) }
    }

    fun setDailyGoal(xp: Int) = update { it.copy(dailyGoalXp = xp) }

    fun resetProgress() = update { Progress(disclaimerAccepted = true) }
}
