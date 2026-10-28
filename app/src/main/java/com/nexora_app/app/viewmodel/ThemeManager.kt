package com.nexora_app.app.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the app's dark mode state.
 *
 * This is a plain singleton object for now (in-memory only — resets on app
 * restart). Once you add Firestore/DataStore persistence, this is where the
 * per-uid load/save logic goes:
 *   - on cold start: read cached value from UserPreferences (DataStore)
 *   - after login/signup: read users/{uid}/settings/darkMode from Firestore,
 *     call setDarkTheme() with it BEFORE navigating to Home
 *   - on toggle: setDarkTheme() + write to DataStore + Firestore (merge write
 *     targeting only settings.darkMode, so it doesn't clobber isPremium/
 *     premiumPlan or fields the iOS app owns)
 *   - on logout/account deletion: reset() to clear cached state
 */
object ThemeManager {

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
    }

    fun toggle() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun reset() {
        _isDarkTheme.value = false
    }
}