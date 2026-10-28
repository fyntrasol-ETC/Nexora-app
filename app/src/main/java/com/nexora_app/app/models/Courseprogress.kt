package com.nexora_app.app.models

data class CourseProgress(
    val completedChapterIds: List<Int> = emptyList(),
    /** chapterNumber -> score percentage (0-100). Only set for dialogue+quiz chapters. */
    val exerciseScores: Map<Int, Int> = emptyMap()
)

/** Correct/total from a single quiz run — carried into Completion and persisted from there. */
data class QuizScore(val correct: Int, val total: Int) {
    val percentage: Int
        get() = if (total == 0) 0 else (correct * 100) / total
}