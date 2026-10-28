package com.nexora_app.app.viewmodel

import com.nexora_app.app.models.Word

/**
 * Compose Navigation route args are strings — they can't carry a
 * List<Word> directly. When StartLearningScreen rolls a random 10-word
 * set for a difficulty, it stashes that exact list here keyed by source,
 * so FlashcardDeckScreen reads the SAME list it navigated with instead of
 * re-rolling (which would desync from what the person just tapped).
 *
 * Deliberately process-lifetime only, not persisted — a fresh "Study
 * words" tap always starts a fresh random set, same as iOS's behavior
 * where the deck is built fresh each time StartLearningScreen calls
 * onSelectDifficulty.
 */
object FlashcardWordCache {
    private val cache = mutableMapOf<String, List<Word>>()

    fun put(source: String, words: List<Word>) {
        cache[source] = words
    }

    fun get(source: String): List<Word>? = cache[source]

    fun clear(source: String) {
        cache.remove(source)
    }
}