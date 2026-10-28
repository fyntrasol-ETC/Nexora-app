package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora_app.app.data.AuthRepository
import com.nexora_app.app.data.WeeklyActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

enum class DayState { COMPLETED, MISSED, TODAY, UPCOMING }

data class WeeklyDayUi(
    val date: LocalDate,
    val label: String,
    val state: DayState
)

data class WeeklyCheckInUiState(
    val days: List<WeeklyDayUi> = emptyList(),
    val checkedInCount: Int = 0,
    val isTodayCheckedIn: Boolean = false,
    val isCheckingIn: Boolean = false
)

class WeeklyCheckInViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val weeklyActivityRepository: WeeklyActivityRepository = WeeklyActivityRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyCheckInUiState())
    val uiState: StateFlow<WeeklyCheckInUiState> = _uiState.asStateFlow()

    // Sunday-start week (S M T W T F S) — matches the day-pill order already
    // on the Profile screen (and on iOS's ProfileScreen.swift). This is
    // intentionally independent of HomeScreen's Monday-start weekly-progress
    // bars, which is a separate card with its own convention.
    private val weekDates: List<LocalDate> = run {
        val today = LocalDate.now(ZoneOffset.UTC)
        // DayOfWeek.value is Mon=1..Sun=7, so Sun%7 == 0, which is what
        // lets this formula land on "this week's Sunday" for every day.
        val sunday = today.minusDays(today.dayOfWeek.value.toLong() % 7L)
        (0..6).map { sunday.plusDays(it.toLong()) }
    }

    init {
        val uid = authRepository.currentUid
        if (uid != null) {
            viewModelScope.launch {
                weeklyActivityRepository.observeCheckedInDates(uid, weekDates).collect { checkedInDates ->
                    _uiState.value = buildState(checkedInDates)
                }
            }
        } else {
            _uiState.value = buildState(emptySet())
        }
    }

    private fun buildState(checkedInDates: Set<LocalDate>): WeeklyCheckInUiState {
        val today = LocalDate.now(ZoneOffset.UTC)
        val days = weekDates.map { date ->
            val state = when {
                checkedInDates.contains(date) -> DayState.COMPLETED
                date == today -> DayState.TODAY
                date.isBefore(today) -> DayState.MISSED
                else -> DayState.UPCOMING
            }
            WeeklyDayUi(date = date, label = dayLabel(date), state = state)
        }
        return WeeklyCheckInUiState(
            days = days,
            checkedInCount = days.count { it.state == DayState.COMPLETED },
            isTodayCheckedIn = checkedInDates.contains(today)
        )
    }

    private fun dayLabel(date: LocalDate): String =
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

    fun checkInToday() {
        val uid = authRepository.currentUid ?: return
        if (_uiState.value.isTodayCheckedIn || _uiState.value.isCheckingIn) return
        _uiState.value = _uiState.value.copy(isCheckingIn = true)
        viewModelScope.launch {
            try {
                weeklyActivityRepository.checkInToday(uid)
                // No manual state flip needed here — checkInToday() writes
                // the doc, the observeCheckedInDates() listener above picks
                // it up, and buildState() recomputes isTodayCheckedIn from
                // that. This keeps a single source of truth (Firestore)
                // instead of the UI optimistically guessing.
            } finally {
                _uiState.value = _uiState.value.copy(isCheckingIn = false)
            }
        }
    }
}