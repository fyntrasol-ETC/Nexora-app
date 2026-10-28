package com.nexora_app.app.screens.courses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexora_app.app.models.QuizQuestion

class ExerciseQuizController(val questions: List<QuizQuestion>) {
    var currentIndex by mutableStateOf(0)
        private set
    var selectedOptionIndex by mutableStateOf<Int?>(null)
        private set
    var correctCount by mutableStateOf(0)
        private set

    val currentQuestion: QuizQuestion get() = questions[currentIndex]
    val isLastQuestion: Boolean get() = currentIndex == questions.lastIndex
    val hasAnswered: Boolean get() = selectedOptionIndex != null
    val progressLabel: String get() = "Question ${currentIndex + 1} of ${questions.size}"

    fun select(index: Int) {
        selectedOptionIndex = index
    }

    /**
     * Advances to the next question, tallying whether the just-answered
     * question was correct. Returns true once the quiz is finished (called
     * on the last question) so the screen knows to navigate to completion
     * with the final score instead of showing another question.
     */
    fun advanceOrFinish(): Boolean {
        if (selectedOptionIndex == currentQuestion.correctOptionIndex) {
            correctCount += 1
        }
        if (isLastQuestion) return true
        currentIndex += 1
        selectedOptionIndex = null
        return false
    }
}