package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora_app.app.data.AuthRepository
import com.nexora_app.app.data.UserRepository
import com.nexora_app.app.models.AvatarOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val isLoading: Boolean = true,
    val storedName: String = "",
    val nameInput: String = "",
    val avatarSeed: String = AvatarOptions.DEFAULT_SEED,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    /** Drives the Save button's enabled state. */
    val isNameChanged: Boolean
        get() = nameInput.isNotBlank() && nameInput != storedName
}

class EditProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        val uid = authRepository.currentUid
        if (uid == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, error = "Not signed in.")
        } else {
            userRepository.observeUser(uid)
                .onEach { user ->
                    if (user != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            storedName = user.displayName,
                            // Only seed nameInput while it's still blank, so
                            // a live Firestore update doesn't stomp on text
                            // the person is actively typing.
                            nameInput = _uiState.value.nameInput.ifBlank { user.displayName },
                            avatarSeed = AvatarOptions.resolveSeed(user.avatarSeed)
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(nameInput = value)
    }

    fun save() {
        val uid = authRepository.currentUid ?: return
        val state = _uiState.value
        if (!state.isNameChanged) return

        _uiState.value = state.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val success = userRepository.updateProfile(uid, displayName = state.nameInput)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                storedName = if (success) state.nameInput else _uiState.value.storedName,
                error = if (success) null else "Could not save your name. Please try again."
            )
        }
    }

    /** Persists immediately — per the spec, avatar changes aren't gated by Save. */
    fun selectAvatar(seed: String) {
        val uid = authRepository.currentUid ?: return
        _uiState.value = _uiState.value.copy(avatarSeed = seed)
        viewModelScope.launch {
            userRepository.updateProfile(uid, avatarSeed = seed)
        }
    }
}