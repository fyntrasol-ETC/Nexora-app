package com.nexora_app.app.data

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


class WeeklyActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun dateId(date: LocalDate): String = date.format(dateFormatter)

    private fun weeklyActivityCollection(uid: String) =
        firestore.collection("users").document(uid).collection("weeklyActivity")

    /** Marks today as active/checked-in. Idempotent — safe to call again if already checked in. */
    suspend fun checkInToday(uid: String) {
        val today = LocalDate.now(ZoneOffset.UTC)
        weeklyActivityCollection(uid)
            .document(dateId(today))
            .set(
                mapOf(
                    "active" to true,
                    "checkedInAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    /**
     * Live-observes which of the given [dates] are checked in (active == true).
     * Emits the subset of [dates] that currently have active == true; any
     * date with no doc (or active == false) is simply absent from the set.
     *
     * Firestore's document-ID whereIn supports up to 10 values, so a 7-day
     * week fits in a single query/listener.
     */
    fun observeCheckedInDates(uid: String, dates: List<LocalDate>): Flow<Set<LocalDate>> = callbackFlow {
        if (dates.isEmpty()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val idToDate = dates.associateBy { dateId(it) }
        val registration: ListenerRegistration = weeklyActivityCollection(uid)
            .whereIn(FieldPath.documentId(), idToDate.keys.toList())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val checkedIn = snapshot?.documents
                    ?.filter { it.getBoolean("active") == true }
                    ?.mapNotNull { idToDate[it.id] }
                    ?.toSet()
                    ?: emptySet()
                trySend(checkedIn)
            }
        awaitClose { registration.remove() }
    }
}