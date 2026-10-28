package com.nexora_app.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Persists per-user video state to users/{uid}/videoActivity/{videoId}.
 * Currently only handles the favorited flag — `watched`,
 * `lastPositionSeconds`, and `watchCount` already exist in the schema for
 * later playback-tracking work, and this writes with merge so favoriting
 * never clobbers those once that lands.
 *
 * Mirrors iOS's VideoActivityService.swift field-for-field.
 */
class VideoActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun collection(uid: String) =
        firestore.collection("users").document(uid).collection("videoActivity")

    /** Fetches the set of videoIds currently marked favorited for this user. */
    suspend fun fetchFavoriteIds(uid: String): Set<String> {
        val snapshot = collection(uid).get().await()
        return snapshot.documents
            .filter { it.getBoolean("isFavorited") == true }
            .map { it.id }
            .toSet()
    }

    /**
     * Create-or-merge a video's favorited state. Deliberately does NOT
     * touch `lastWatchedAt` — favoriting a video isn't the same as
     * watching it, and the rules don't require that field on every write.
     * Swallows failures — local state already reflects the toggle by the
     * time this is called, so a dropped write shouldn't surface as a UI
     * error; the next toggle on this video will retry it.
     */
    suspend fun setFavorited(uid: String, videoId: String, isFavorited: Boolean) {
        try {
            val doc = collection(uid).document(videoId)
            if (isFavorited) {
                doc.set(hashMapOf("isFavorited" to true), SetOptions.merge()).await()
            } else {
                doc.delete().await()
            }
        } catch (e: Exception) {
            // Non-fatal — see kdoc above.
        }
    }
}