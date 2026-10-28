package com.nexora_app.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora_app.app.data.AuthRepository
import com.nexora_app.app.data.CourseProgressRepository
import com.nexora_app.app.models.CourseProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs CoursesScreen's per-course progress bars. Progress is keyed by
 * courseId; a course with no entry in the map has never been started.
 */
class CoursesViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val progressRepository: CourseProgressRepository = CourseProgressRepository()
) : ViewModel() {

    private val _progressByCourseId = MutableStateFlow<Map<String, CourseProgress>>(emptyMap())
    val progressByCourseId: StateFlow<Map<String, CourseProgress>> = _progressByCourseId.asStateFlow()

    init {
        authRepository.currentUid?.let { uid ->
            viewModelScope.launch {
                progressRepository.observeAllProgress(uid).collect { map ->
                    _progressByCourseId.value = map
                }
            }
        }
    }

    /** Called once when a chapter's Completion screen is reached. */
    fun markChapterComplete(courseId: String, chapterNumber: Int) {
        val uid = authRepository.currentUid ?: return
        viewModelScope.launch {
            progressRepository.markChapterComplete(uid, courseId, chapterNumber)
        }
    }
}