package com.nexora_app.app.screens.videos

import com.nexora_app.app.data.VideoActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Holds the set of favorited video ids and mirrors it to
 * users/{uid}/videoActivity/{videoId} via VideoActivityRepository — same
 * role as iOS's VideoFeedViewModel.favoriteIDs.
 *
 * Kept as a singleton `object` (not an androidx ViewModel) since it's a
 * simple cross-screen cache, matching the existing style of AudioManager /
 * ThemeManager elsewhere in viewmodel/. Its own CoroutineScope (rather
 * than viewModelScope) is what makes the persistence calls in toggle()
 * possible from an object with no natural teardown point.
 */
object VideoFavoritesRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val activityRepository = VideoActivityRepository()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private var uid: String? = null

    /**
     * Call once the signed-in user's uid is known (e.g. from
     * VideoFeedScreen's LaunchedEffect, reading authViewModel.currentUid).
     * Fetches this user's favorited videos from Firestore. Safe to call
     * again on re-sign-in — it just re-fetches.
     */
    suspend fun configure(uid: String) {
        this.uid = uid
        try {
            _favoriteIds.value = activityRepository.fetchFavoriteIds(uid)
        } catch (e: Exception) {
            // Non-fatal — the feed still works locally even if this
            // particular fetch failed (e.g. offline on first launch).
        }
    }

    fun toggle(videoId: String) {
        val current = _favoriteIds.value
        val isNowFavorited = videoId !in current
        _favoriteIds.value = if (isNowFavorited) current + videoId else current - videoId

        val currentUid = uid ?: return
        scope.launch {
            activityRepository.setFavorited(currentUid, videoId, isNowFavorited)
        }
    }

    fun isFavorite(videoId: String): Boolean = videoId in _favoriteIds.value
}