package com.nexora_app.app.models

import com.google.firebase.Timestamp

/**
 * Mirrors users/{uid} in Firestore exactly — field names/types here must stay
 * in sync with firestore.rules and with the iOS side. See
 * ELLIE_PROJECT_CONTEXT.md for the full schema doc.
 */
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoURL: String? = null,
    val nativeLanguage: String? = null,
    val createdAt: Timestamp? = null,
    val totalTimeSpentSeconds: Long = 0L,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    // Seed string for a DiceBear avatar (see models/AvatarOptions.kt).
    // MIGRATION: replaces the old avatarId field (a MascotAvatarType case
    // id like "bear_gold").
    val avatarSeed: String = AvatarOptions.DEFAULT_SEED
)