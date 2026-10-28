package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora_app.app.data.AuthRepository
import com.nexora_app.app.data.UserRepository
import com.nexora_app.app.models.AvatarOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val avatarSeed: String = AvatarOptions.DEFAULT_SEED,
    val timeSpentLabel: String = "0h 0min",
    val badgesCount: String = "0",
    val wordsKnownCount: String = "0",
    val isLoading: Boolean = true
)

class ProfileViewModel(
    authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUid

        if (uid == null) {
            // Shouldn't normally happen — ProfileScreen only renders behind
            // the signed-in part of the nav graph — but fail quietly rather
            // than crash if it's ever reached in a bad state.
            _uiState.value = _uiState.value.copy(isLoading = false)
        } else {
            viewModelScope.launch {
                userRepository.observeUser(uid).collect { user ->
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            displayName = user.displayName,
                            // Normalized to lowercase for display regardless
                            // of what case the Firestore doc has stored —
                            // Firebase Auth sign-in is case-insensitive, so
                            // an old account created as "Zainab@gmail.com"
                            // would otherwise keep showing capitalized
                            // forever even though sign-in itself is fine.
                            email = user.email.lowercase(),
                            avatarSeed = AvatarOptions.resolveSeed(user.avatarSeed),
                            timeSpentLabel = formatTimeSpent(user.totalTimeSpentSeconds),
                            isLoading = false
                        )
                    }
                }
            }

            // Counts are separate aggregation queries (cheap — they read a
            // count, not every doc), so they don't block the name/time-spent
            // from showing up first.
            viewModelScope.launch {
                val badges = userRepository.badgeCount(uid)
                _uiState.value = _uiState.value.copy(badgesCount = badges.toString())
            }
            viewModelScope.launch {
                val known = userRepository.knownWordsCount(uid)
                _uiState.value = _uiState.value.copy(wordsKnownCount = known.toString())
            }
        }
    }

    private fun formatTimeSpent(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return "${hours}h ${minutes}min"
    }
}