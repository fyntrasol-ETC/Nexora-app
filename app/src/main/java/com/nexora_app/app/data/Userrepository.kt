package com.nexora_app.app.data

import com.nexora_app.app.models.AvatarOptions
import com.nexora_app.app.models.User
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Read side of users/{uid} — separate from AuthRepository, which only
 * writes/creates it. Field names must stay in sync with ELLIE_PROJECT_CONTEXT.md.
 */
class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Live updates to users/{uid}. Using a snapshot listener rather than a
     * one-time get() means the name/streak/time-spent shown here stays
     * current if edited on the other platform while this screen is open.
     */
    fun observeUser(uid: String): Flow<User?> = callbackFlow {
        val listener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val user = snapshot?.takeIf { it.exists() }?.let {
                    User(
                        uid = it.id,
                        displayName = it.getString("displayName") ?: "",
                        email = it.getString("email") ?: "",
                        photoURL = it.getString("photoURL"),
                        nativeLanguage = it.getString("nativeLanguage"),
                        createdAt = it.getTimestamp("createdAt"),
                        totalTimeSpentSeconds = it.getLong("totalTimeSpentSeconds") ?: 0L,
                        currentStreak = (it.getLong("currentStreak") ?: 0L).toInt(),
                        longestStreak = (it.getLong("longestStreak") ?: 0L).toInt(),
                        avatarSeed = it.getString("avatarSeed") ?: AvatarOptions.DEFAULT_SEED
                    )
                }
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    /** Count aggregation query — reads a count, not every badge doc. */
    suspend fun badgeCount(uid: String): Int {
        return try {
            firestore.collection("users").document(uid).collection("badges")
                .count().get(AggregateSource.SERVER).await().count.toInt()
        } catch (e: Exception) {
            0
        }
    }

    /** Same idea, filtered to status == "know". */
    suspend fun knownWordsCount(uid: String): Int {
        return try {
            firestore.collection("users").document(uid).collection("words")
                .whereEqualTo("status", "know")
                .count().get(AggregateSource.SERVER).await().count.toInt()
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Partial update of users/{uid} — only the fields you pass are written,
     * so this is safe to call from both the name Save flow (displayName
     * only) and the avatar picker (avatarSeed only) without clobbering the
     * other. Returns false on failure so callers can surface an error
     * instead of silently pretending the write succeeded.
     */
    suspend fun updateProfile(
        uid: String,
        displayName: String? = null,
        avatarSeed: String? = null
    ): Boolean {
        val updates = mutableMapOf<String, Any>()
        displayName?.let { updates["displayName"] = it }
        avatarSeed?.let { updates["avatarSeed"] = it }
        if (updates.isEmpty()) return true

        return try {
            firestore.collection("users").document(uid).update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}