package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora_app.app.data.WordProgressRepository
import com.nexora_app.app.models.Word
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Single source of truth for word status + saved state — mirrors iOS
 * WordsViewModel. WordsScreen, StartLearningScreen, and FlashcardDeckScreen
 * all read/write through ONE instance of this (scoped to the "words_graph"
 * nested nav graph in NavGraph.kt) so "I Know" / "I Don't Know" / Saved
 * stay in sync no matter which screen made the change.
 *
 * Every mutation that changes status or isSaved is mirrored to
 * users/{uid}/words/{wordId} via WordProgressRepository, so the same state
 * survives relaunch and shows up correctly on the iOS side too.
 */
class WordsViewModel(
    initialWords: List<Word> = Word.sampleWords,
    private val progressRepository: WordProgressRepository = WordProgressRepository()
) : ViewModel() {

    private val _words = MutableStateFlow(initialWords)

    private val _showSavedOnly = MutableStateFlow(false)
    val showSavedOnly: StateFlow<Boolean> = _showSavedOnly.asStateFlow()

    private val _sortAlphabetically = MutableStateFlow(false)
    val sortAlphabetically: StateFlow<Boolean> = _sortAlphabetically.asStateFlow()

    private var uid: String? = null

    /**
     * Call once the signed-in user's uid is known (e.g. from WordsScreen's
     * LaunchedEffect, reading AuthRepository().currentUid). Pulls this
     * user's saved/known/don't-know state from Firestore and merges it
     * onto the bundled word catalog. Safe to call again on re-sign-in — it
     * just re-fetches and re-merges.
     */
    suspend fun configure(uid: String) {
        this.uid = uid
        try {
            val progress = progressRepository.fetchAll(uid)
            _words.value = _words.value.map { word ->
                progress[word.id]?.let { word.copy(status = it.status, isSaved = it.isSaved) } ?: word
            }
        } catch (e: Exception) {
            // Non-fatal — the bundled catalog still works locally even if
            // this particular fetch failed (e.g. offline on first launch).
        }
    }

    // ---- Buckets shown on WordsScreen ----
    // Both are empty until a word has actually been swiped/tapped on a
    // flashcard — words sitting at UNKNOWN show up in neither.
    val knownWords: StateFlow<List<Word>> = combine(_words, _showSavedOnly, _sortAlphabetically) { words, savedOnly, sorted ->
        display(words.filter { it.status == Word.Status.KNOW }, savedOnly, sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unknownWords: StateFlow<List<Word>> = combine(_words, _showSavedOnly, _sortAlphabetically) { words, savedOnly, sorted ->
        display(words.filter { it.status == Word.Status.DONT_KNOW }, savedOnly, sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun display(list: List<Word>, savedOnly: Boolean, sorted: Boolean): List<Word> {
        val filtered = if (savedOnly) list.filter { it.isSaved } else list
        return if (sorted) filtered.sortedBy { it.text } else filtered
    }

    fun toggleShowSavedOnly() {
        _showSavedOnly.value = !_showSavedOnly.value
    }

    fun toggleSortAlphabetically() {
        _sortAlphabetically.value = !_sortAlphabetically.value
    }

    /**
     * Backs the Beginner / Intermediate / Advanced buttons on
     * StartLearningScreen. Prioritizes words at that level not yet studied
     * (still UNKNOWN) so a session isn't just re-serving words already
     * marked know/don't know. Falls back to the full level pool once
     * everything's been studied at least once.
     */
    fun randomWords(difficulty: Word.Difficulty, count: Int = 10): List<Word> {
        val levelWords = _words.value.filter { it.difficulty == difficulty }
        val unstudied = levelWords.filter { it.status == Word.Status.UNKNOWN }
        val pool = unstudied.ifEmpty { levelWords }
        return pool.shuffled().take(count)
    }

    /** All currently-known words — backs "Words I know" focused study mode. */
    fun allKnownWords(): List<Word> = _words.value.filter { it.status == Word.Status.KNOW }

    /** All currently-not-known words — backs "Words I don't know" focused study mode. */
    fun allDontKnowWords(): List<Word> = _words.value.filter { it.status == Word.Status.DONT_KNOW }

    /** Looks a word up by id — used when FlashcardDeckScreen needs the latest state for a word. */
    fun wordById(id: String): Word? = _words.value.firstOrNull { it.id == id }

    // ---- Mutations ----

    fun markKnown(word: Word) = setStatus(word, Word.Status.KNOW)
    fun markDontKnow(word: Word) = setStatus(word, Word.Status.DONT_KNOW)

    fun toggleSaved(word: Word) {
        var updated: Word? = null
        _words.value = _words.value.map {
            if (it.id == word.id) {
                val next = it.copy(isSaved = !it.isSaved)
                updated = next
                next
            } else it
        }
        updated?.let { persist(it) }
    }

    /**
     * Removes a word entirely — used by swipe-to-delete on WordsScreen. A
     * deleted word disappears from both buckets and is gone for good, as
     * opposed to markKnown/markDontKnow which just change its status. Also
     * deletes the matching Firestore doc, so it doesn't silently reappear
     * on next sign-in with stale status.
     */
    fun deleteWord(word: Word) {
        _words.value = _words.value.filterNot { it.id == word.id }

        val currentUid = uid ?: return
        viewModelScope.launch {
            progressRepository.delete(currentUid, word.id)
        }
    }

    private fun setStatus(word: Word, status: Word.Status) {
        var updated: Word? = null
        _words.value = _words.value.map {
            if (it.id == word.id) {
                val next = it.copy(status = status)
                updated = next
                next
            } else it
        }
        updated?.let { persist(it) }
    }

    /**
     * Mirrors a word's current state to Firestore. Per the schema note —
     * only write once a word has left UNKNOWN or been saved, so we don't
     * pre-create empty docs for words nobody's touched yet.
     */
    private fun persist(word: Word) {
        val currentUid = uid ?: return
        if (word.status == Word.Status.UNKNOWN && !word.isSaved) return
        viewModelScope.launch {
            progressRepository.upsert(currentUid, word)
        }
    }
}