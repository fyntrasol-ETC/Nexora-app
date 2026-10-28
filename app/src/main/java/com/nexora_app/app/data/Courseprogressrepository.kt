package com.nexora_app.app.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.nexora_app.app.models.CourseProgress
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Read/write for users/{uid}/courseProgress/{courseId}. Field names must
 * stay in sync with firestore.rules (see ELLIE_PROJECT_CONTEXT.md).
 */
class CourseProgressRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Live map of courseId -> CourseProgress for every course doc the user
     * has touched. A course the user hasn't started yet simply has no key
     * here — callers should treat a missing entry as "0% complete", not
     * an error.
     */
    fun observeAllProgress(uid: String): Flow<Map<String, CourseProgress>> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .collection("courseProgress")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }
                val map = snapshot?.documents?.associate { doc ->
                    val completedIds = (doc.get("completedChapterIds") as? List<*>)
                        ?.mapNotNull { (it as? Number)?.toInt() }
                        ?: emptyList()
                    // Firestore map keys are always strings on the wire —
                    // parse "3" -> 3 for the chapter number.
                    val scores = (doc.get("exerciseScores") as? Map<*, *>)
                        ?.entries
                        ?.mapNotNull { (k, v) ->
                            val chapterNumber = (k as? String)?.toIntOrNull()
                            val percentage = (v as? Number)?.toInt()
                            if (chapterNumber != null && percentage != null) chapterNumber to percentage else null
                        }
                        ?.toMap()
                        ?: emptyMap()
                    doc.id to CourseProgress(completedChapterIds = completedIds, exerciseScores = scores)
                }.orEmpty()
                trySend(map)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Marks one chapter finished. Uses arrayUnion so completing an
     * already-completed chapter again (e.g. a retake, or a re-entrant
     * Completion route) is a harmless no-op instead of adding a duplicate
     * entry — the tick stays regardless of how many times this fires.
     *
     * scorePercentage, when present, is written with a dotted field path
     * ("exerciseScores.<chapterNumber>") so set(..., merge = true) updates
     * only that one chapter's score without touching any other chapter's
     * entry already in the map — a retake just overwrites its own score.
     */
    suspend fun markChapterComplete(
        uid: String,
        courseId: String,
        chapterNumber: Int,
        scorePercentage: Int? = null
    ) {
        val updates = mutableMapOf<String, Any>(
            "completedChapterIds" to FieldValue.arrayUnion(chapterNumber),
            "lastAccessedAt" to FieldValue.serverTimestamp()
        )
        if (scorePercentage != null) {
            updates["exerciseScores.$chapterNumber"] = scorePercentage
        }

        firestore.collection("users").document(uid)
            .collection("courseProgress").document(courseId)
            .set(updates, SetOptions.merge())
            .await()
    }
}