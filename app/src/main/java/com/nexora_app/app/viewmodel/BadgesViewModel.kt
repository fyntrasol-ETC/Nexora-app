package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nexora_app.app.models.BadgeCatalog
import com.nexora_app.app.models.CourseCatalog
import com.nexora_app.app.models.isCourseFullyComplete
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BadgesUiState(
    val progressByCourseId: Map<String, List<Int>> = emptyMap(),
    val unlockedBadgeIds: Set<String> = emptySet(),
    val isLoading: Boolean = true
)

class BadgesViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid

    private val _uiState = MutableStateFlow(BadgesUiState())
    val uiState: StateFlow<BadgesUiState> = _uiState.asStateFlow()

    private var progressListener: ListenerRegistration? = null
    private var badgesListener: ListenerRegistration? = null

    // Rules make badges write-once (create only), so a repeat create() for
    // an already-earned badge would just fail — this only stops us from
    // firing that doomed call more than once per session.
    private val badgeWriteAttempted = mutableSetOf<String>()

    init {
        listenToProgress()
        listenToBadges()
    }

    private fun listenToProgress() {
        val currentUid = uid ?: return
        progressListener = db.collection("users").document(currentUid)
            .collection("courseProgress")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val progress = snapshot.documents.associate { doc ->
                    val ids = (doc.get("completedChapterIds") as? List<*>)
                        ?.mapNotNull { (it as? Number)?.toInt() }
                        ?: emptyList()
                    doc.id to ids
                }
                _uiState.value = _uiState.value.copy(progressByCourseId = progress, isLoading = false)
                awardAnyNewlyCompletedBadges(progress)
            }
    }

    private fun listenToBadges() {
        val currentUid = uid ?: return
        badgesListener = db.collection("users").document(currentUid)
            .collection("badges")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                _uiState.value = _uiState.value.copy(
                    unlockedBadgeIds = snapshot.documents.map { it.id }.toSet()
                )
            }
    }

    private fun awardAnyNewlyCompletedBadges(progress: Map<String, List<Int>>) {
        val currentUid = uid ?: return
        val alreadyUnlocked = _uiState.value.unlockedBadgeIds
        CourseCatalog.courses.forEach { course ->
            val completed = progress[course.id] ?: return@forEach
            if (!isCourseFullyComplete(course, completed)) return@forEach
            val badge = BadgeCatalog.badgeFor(course.id) ?: return@forEach
            if (badge.id in alreadyUnlocked || badge.id in badgeWriteAttempted) return@forEach
            badgeWriteAttempted += badge.id
            db.collection("users").document(currentUid)
                .collection("badges").document(badge.id)
                .set(mapOf("earnedAt" to Timestamp.now()))
        }
    }

    override fun onCleared() {
        progressListener?.remove()
        badgesListener?.remove()
    }
}