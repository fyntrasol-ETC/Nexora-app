package com.nexora_app.app.data

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Wraps FirebaseAuth + Firestore for everything under users/{uid}.
 * Field names written here must match firestore.rules exactly (see
 * ELLIE_PROJECT_CONTEXT.md) or writes get rejected.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUid: String?
        get() = auth.currentUser?.uid

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Live stream of "is someone signed in right now" — true/false on every
     * FirebaseAuth session change (sign in, sign out, token expiry/deletion).
     * Firebase persists the session on disk by default, so this already
     * reflects a still-logged-in user across app restarts with no extra
     * work — SplashScreen just needs to read it once before routing.
     */
    val authStateFlow: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Firebase Auth itself matches sign-in emails case-insensitively, but
     * whatever string we hand it (and whatever we write to Firestore) is
     * kept verbatim otherwise. Without this, an account created as
     * "Zainab@gmail.com" stores that exact casing in users/{uid}.email
     * forever, even though signing in with "zainab@gmail.com" works fine —
     * which is exactly the display bug this fixes at the source. Trimming
     * also guards against a stray leading/trailing space from the keyboard
     * turning into a silently-different-looking stored value.
     */
    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    /**
     * Shared shape for a brand-new users/{uid} doc. Both signUp and a
     * first-time Google sign-in need to create this — keep them identical
     * or the two paths silently drift apart.
     */
    private fun newUserDoc(displayName: String, email: String) = hashMapOf(
        "displayName" to displayName,
        "email" to email,
        "createdAt" to FieldValue.serverTimestamp(),
        "totalTimeSpentSeconds" to 0L,
        "currentStreak" to 0,
        "longestStreak" to 0
    )

    // ---- Sign up ----------------------------------------------------------

    suspend fun signUp(displayName: String, email: String, password: String): AuthResult {
        val normalizedEmail = normalizeEmail(email)
        return try {
            val result = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("Could not create account.")

            try {
                firestore.collection("users").document(uid)
                    .set(newUserDoc(displayName, normalizedEmail)).await()
            } catch (e: Exception) {
                // Auth account exists but the profile doc failed to write.
                // Surface this distinctly so the UI can retry doc creation
                // rather than sending the user back through sign-up with an
                // email that Firebase now considers taken.
                return AuthResult.Error("Account created, but saving your profile failed: ${e.message}")
            }

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed.")
        }
    }

    // ---- Sign in ------------------------------------------------------------

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(normalizeEmail(email), password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign in failed.")
        }
    }

    // ---- Google sign-in ---------------------------------------------------
    //
    // Google sign-in skips signUp entirely, so a first-time Google user has
    // no users/{uid} doc yet. We check additionalUserInfo?.isNewUser and
    // create the same doc shape signUp does, so every account — password or
    // Google — ends up with a matching Firestore profile.

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return AuthResult.Error("Google sign-in failed.")

            if (result.additionalUserInfo?.isNewUser == true) {
                val displayName = user.displayName ?: "Learner"
                val email = normalizeEmail(user.email ?: "")

                try {
                    firestore.collection("users").document(user.uid)
                        .set(newUserDoc(displayName, email)).await()
                } catch (e: Exception) {
                    return AuthResult.Error("Signed in, but saving your profile failed: ${e.message}")
                }
            }

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign-in failed.")
        }
    }

    // ---- Password reset -------------------------------------------------

    suspend fun sendPasswordReset(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(normalizeEmail(email)).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not send reset link.")
        }
    }

    // ---- Change password (requires reauth) -------------------------------

    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("Not signed in.")
        val email = user.email ?: return AuthResult.Error("No email on this account.")
        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not update password.")
        }
    }

    // ---- Sign out -----------------------------------------------------------

    fun signOut() {
        auth.signOut()
    }

    // ---- Delete account -------------------------------------------------
    //
    // Spark plan = no Cloud Functions, so nothing cleans up subcollections
    // automatically. This must run in exactly this order:
    //   1. reauthenticate (delete/updatePassword require a *recent* login)
    //   2. batch-delete every subcollection under users/{uid}
    //   3. delete the users/{uid} doc itself
    //   4. call FirebaseAuth's deleteUser()
    // Reordering this breaks it: rules check isOwner(uid) against
    // request.auth, which stops existing the instant the Auth user is
    // deleted, so subcollection deletes after that point get rejected and
    // you're left with an orphaned users/{uid} doc.
    //
    // NOTE: for accounts created via Google sign-in, EmailAuthProvider
    // reauth won't work since there's no password on file — this method
    // still assumes a password account. See the note below the class if
    // you need Google-account deletion too.

    private val subcollections = listOf(
        "courseProgress", "words", "videoActivity", "badges", "weeklyActivity"
    )

    suspend fun deleteAccount(currentPassword: String): AuthResult {
        val user = auth.currentUser ?: return AuthResult.Error("Not signed in.")
        val email = user.email ?: return AuthResult.Error("No email on this account.")
        val uid = user.uid

        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()

            for (name in subcollections) {
                deleteAllDocsIn(firestore.collection("users").document(uid).collection(name))
            }

            firestore.collection("users").document(uid).delete().await()
            user.delete().await()

            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not delete account.")
        }
    }

    private suspend fun deleteAllDocsIn(collection: CollectionReference) {
        // Batches cap at 500 writes — paginate defensively in case any one
        // subcollection (e.g. words) grows past that sfor a long-time user.
        while (true) {
            val snapshot = collection.limit(400).get().await()
            if (snapshot.isEmpty) break
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}