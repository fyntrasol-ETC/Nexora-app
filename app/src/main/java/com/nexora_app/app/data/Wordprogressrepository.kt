package com.nexora_app.app.data

import com.nexora_app.app.models.Word
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class WordProgress(val status: Word.Status, val isSaved: Boolean)

/**
 * Persists per-user word status/saved state to users/{uid}/words/{wordId},
 * matching firestore.rules. The word catalog itself (text, definition,
 * example, etc.) stays bundled in Word.kt on both platforms — Firestore
 * only stores the per-user status/isSaved delta, keyed by the same stable
 * Word.id ("b1", "i1", "a1"...) used in the bundled catalog.
 *
 * Mirrors iOS's WordProgressService.swift field-for-field.
 */
class WordProgressRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("words")

    /** Fetches every per-user word-progress doc, keyed by wordId, for the caller to merge onto the bundled catalog. */
    suspend fun fetchAll(uid: String): Map<String, WordProgress> {
        val snapshot = collection(uid).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val status = when (doc.getString("status")) {
                "know" -> Word.Status.KNOW
                "dontKnow" -> Word.Status.DONT_KNOW
                else -> Word.Status.UNKNOWN
            }
            val isSaved = doc.getBoolean("isSaved") ?: false
            doc.id to WordProgress(status, isSaved)
        }.toMap()
    }

    /**
     * Create-or-merge a word's current status/isSaved. Per the schema note
     * in firestore.rules: only call this once a word has actually left
     * UNKNOWN or been saved — don't pre-create empty docs for every
     * bundled word, that's wasted Spark quota for no benefit.
     *
     * Swallows failures rather than throwing — local state already
     * reflects the change by the time this is called, so a dropped write
     * here shouldn't surface as a UI error; the next mutation on this word
     * will just retry it.
     */
    suspend fun upsert(uid: String, word: Word) {
        val data = hashMapOf(
            "status" to word.status.toFirestoreValue(),
            "isSaved" to word.isSaved,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            collection(uid).document(word.id).set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            // Non-fatal — see kdoc above.
        }
    }

    /** Removes a word's per-user progress doc — called on swipe-to-delete. */
    suspend fun delete(uid: String, wordId: String) {
        try {
            collection(uid).document(wordId).delete().await()
        } catch (e: Exception) {
            // Non-fatal, same reasoning as upsert.
        }
    }

    private fun Word.Status.toFirestoreValue(): String = when (this) {
        Word.Status.UNKNOWN -> "unknown"
        Word.Status.KNOW -> "know"
        Word.Status.DONT_KNOW -> "dontKnow"
    }
}