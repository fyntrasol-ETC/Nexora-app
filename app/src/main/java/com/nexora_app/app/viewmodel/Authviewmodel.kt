package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora_app.app.data.AuthRepository
import com.nexora_app.app.data.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resetLinkSent: Boolean = false
)

/**
 * Single ViewModel behind SignIn, SignUp, ResetPassword, ChangePassword and
 * DeleteAccount — they're all just different faces of the same Auth
 * repository, so one shared UI-state shape keeps loading/error handling
 * consistent across all five screens.
 */
class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isSignedIn: Boolean
        get() = repository.isSignedIn

    val currentUid: String?
        get() = repository.currentUid

    fun signUp(displayName: String, email: String, password: String, onSuccess: () -> Unit) {
        run(onSuccess) { repository.signUp(displayName, email, password) }
    }

    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        run(onSuccess) { repository.signIn(email, password) }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        run(onSuccess) { repository.signInWithGoogle(idToken) }
    }

    fun sendPasswordReset(email: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = repository.sendPasswordReset(email)) {
                is AuthResult.Success ->
                    _uiState.value = _uiState.value.copy(isLoading = false, resetLinkSent = true)
                is AuthResult.Error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit) {
        run(onSuccess) { repository.changePassword(currentPassword, newPassword) }
    }

    fun deleteAccount(currentPassword: String, onSuccess: () -> Unit) {
        run(onSuccess) { repository.deleteAccount(currentPassword) }
    }

    fun signOut() {
        repository.signOut()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun run(onSuccess: () -> Unit, action: suspend () -> AuthResult) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val result = action()) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }
}