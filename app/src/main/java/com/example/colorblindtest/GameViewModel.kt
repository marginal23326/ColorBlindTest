package com.example.colorblindtest

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class Screen { HOME, GAME, RESULT }

data class Question(
    val correctName: String,
    val color: Color,
    val options: List<String>
)

// New data class to store details of an incorrect answer
data class IncorrectAnswer(
    val question: Question,
    val selectedAnswer: String
)

class GameViewModel : ViewModel() {
    companion object {
        const val DEFAULT_TOTAL_QUESTIONS = 10
    }

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    // settings
    private val _totalQuestions = MutableStateFlow(DEFAULT_TOTAL_QUESTIONS)
    val totalQuestions: StateFlow<Int> = _totalQuestions

    // runtime
    private val _currentQuestion = MutableStateFlow(generateQuestion())
    val currentQuestion = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    val correctCount = MutableStateFlow(0)
    val selectedAnswer = MutableStateFlow<String?>(null)
    val answered = MutableStateFlow(false)

    // New state flow for incorrect answers
    private val _incorrectAnswers = MutableStateFlow<List<IncorrectAnswer>>(emptyList())
    val incorrectAnswers: StateFlow<List<IncorrectAnswer>> = _incorrectAnswers.asStateFlow()

    // timing
    val questionStartTime = MutableStateFlow(0L)
    private val times = mutableListOf<Long>()

    fun setTotalQuestions(count: Int) {
        if (count > 0) { // Basic validation
            _totalQuestions.value = count
        }
    }

    fun startGame() {
        // shuffle and prepare
        times.clear()
        _incorrectAnswers.value = emptyList() // Clear incorrect answers
        correctCount.value = 0
        _currentIndex.value = 0
        // _totalQuestions is already set, either to default or by user
        _currentQuestion.value = generateQuestion()
        _screen.value = Screen.GAME
        selectedAnswer.value = null
        answered.value = false
    }

    fun markQuestionStart() {
        questionStartTime.value = System.currentTimeMillis()
    }

    fun submitAnswer(option: String) {
        if (answered.value) return
        val elapsed = System.currentTimeMillis() - questionStartTime.value
        times.add(elapsed)
        selectedAnswer.value = option
        answered.value = true

        val q = _currentQuestion.value
        if (option == q.correctName) {
            correctCount.value = correctCount.value + 1
        } else {
            // Add to incorrect answers list
            val currentIncorrect = _incorrectAnswers.value.toMutableList()
            currentIncorrect.add(IncorrectAnswer(question = q, selectedAnswer = option))
            _incorrectAnswers.value = currentIncorrect
        }
        // move to next after a small delay (UI can trigger). For simplicity, immediate next:
        nextQuestion()
    }

    fun skipQuestion() {
        if (answered.value) return
        times.add( (System.currentTimeMillis() - questionStartTime.value).coerceAtLeast(0L) )
        // no correct count increment
        answered.value = true

        // Add skipped question to incorrect answers list
        val q = _currentQuestion.value
        val currentIncorrect = _incorrectAnswers.value.toMutableList()
        currentIncorrect.add(IncorrectAnswer(question = q, selectedAnswer = "Skipped"))
        _incorrectAnswers.value = currentIncorrect

        nextQuestion()
    }

    private fun nextQuestion() {
        val next = _currentIndex.value + 1
        if (next >= _totalQuestions.value) {
            _screen.value = Screen.RESULT
        } else {
            _currentIndex.value = next
            _currentQuestion.value = generateQuestion()
            selectedAnswer.value = null
            answered.value = false
        }
    }

    fun resetGame() {
        _screen.value = Screen.HOME
    }

    fun computeFinalScore(): Double {
        val total = _totalQuestions.value.coerceAtLeast(1)
        val accuracy = (correctCount.value.toDouble() / total) * 100.0
        // avgTime is in seconds
        val avgTime = if (times.isNotEmpty()) times.average() / 1000.0 else 0.0

        val timeScore: Double
        when {
            avgTime <= 2.0 -> {
                // Perfect time score for 2 seconds or less
                timeScore = 100.0
            }
            avgTime <= 5.0 -> { // 2.0 < avgTime <= 5.0
                // Linearly decrease timeScore from 100 (at 2s) to 0 (at 5s)
                // progress = 1.0 when avgTime is 2.0; progress = 0.0 when avgTime is 5.0
                val progress = (5.0 - avgTime) / (5.0 - 2.0)
                timeScore = progress * 100.0
            }
            avgTime <= 8.0 -> { // 5.0 < avgTime <= 8.0
                // Linearly decrease timeScore from 0 (at 5s) to -100 (at 8s)
                // progress = 1.0 when avgTime is 5.0; progress = 0.0 when avgTime is 8.0
                val progress = (8.0 - avgTime) / (8.0 - 5.0)
                timeScore = (progress * 100.0) - 100.0 // Scales from 0 down to -100
            }
            else -> { // avgTime > 8.0
                // Lowest time score for times greater than 8 seconds
                timeScore = -100.0
            }
        }

        // The final score combines accuracy (75%) and the normalized timeScore (25%)
        // (timeScore + 100.0) / 2.0 normalizes timeScore from [-100, 100] to [0, 100]
        val final = (0.75 * accuracy) + (0.25 * (timeScore + 100.0) / 2.0)
        return final.coerceIn(0.0, 100.0) // Ensure final score is between 0 and 100
    }

    fun summaryText(): String {
        val total = _totalQuestions.value
        val correct = correctCount.value
        val accuracy = if (total > 0) (correct * 100) / total else 0
        val at = if (times.isNotEmpty()) String.format(Locale.US, "%.1f", times.average() / 1000.0) else "N/A"
        val score = computeFinalScore()
        val verdict = when {
            score >= 80 -> "No strong signs of red/green color confusion."
            score >= 60 -> "Some signs of mild red/green confusion — consider testing more carefully."
            else -> "Significant signs of red/green confusion (possible protanopia/protanomaly)."
        }
        return "Correct: $correct / $total\nAccuracy: $accuracy%\nAverage time: ${at}s\n\nVerdict: $verdict"
    }

    private fun generateQuestion(): Question {
        val palette = ColorsData.generateTestColors()
        // Ensure palette is not empty to avoid errors with .random()
        if (palette.isEmpty()) {
            // Fallback or error handling for empty palette
            // This might happen if ColorsData.baseColors is empty or generateTestColors() has an issue
            // For now, let's assume it won't be empty based on current ColorsData.kt
            // but in a real app, you might want a placeholder question or error state.
            return Question("Error", Color.Gray, listOf("Error"))
        }
        val correct = palette.random()
        val namesPool = palette.map { it.name }.toMutableList().apply { remove(correct.name) }
        val options = (listOf(correct.name) + namesPool.shuffled().take(3)).shuffled()
        return Question(correct.name, correct.color, options)
    }
}
