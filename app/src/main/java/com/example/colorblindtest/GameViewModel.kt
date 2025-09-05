package com.example.colorblindtest

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
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

data class IncorrectAnswer(
    val question: Question,
    val selectedAnswer: String
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val DEFAULT_TOTAL_QUESTIONS = 10
        private const val PREFS_NAME = "ColorBlindTestPrefs"
        private const val HIGH_SCORE_KEY = "highScore"
    }

    private val app = getApplication<Application>()
    private val sharedPreferences = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _totalQuestions = MutableStateFlow(DEFAULT_TOTAL_QUESTIONS)
    val totalQuestions: StateFlow<Int> = _totalQuestions

    private val _highScore = MutableStateFlow(0.0)
    val highScore: StateFlow<Double> = _highScore.asStateFlow()

    private val _currentQuestion = MutableStateFlow(generateQuestion())
    val currentQuestion = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    val correctCount = MutableStateFlow(0)
    val selectedAnswer = MutableStateFlow<String?>(null)
    val answered = MutableStateFlow(false)

    private val _incorrectAnswers = MutableStateFlow<List<IncorrectAnswer>>(emptyList())
    val incorrectAnswers: StateFlow<List<IncorrectAnswer>> = _incorrectAnswers.asStateFlow()

    val questionStartTime = MutableStateFlow(0L)
    private val times = mutableListOf<Long>()

    init {
        _highScore.value = sharedPreferences.getFloat(HIGH_SCORE_KEY, 0f).toDouble()
    }

    fun setTotalQuestions(count: Int) {
        if (count > 0) {
            _totalQuestions.value = count
        }
    }

    fun startGame() {
        times.clear()
        _incorrectAnswers.value = emptyList()
        correctCount.value = 0
        _currentIndex.value = 0
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
            val currentIncorrect = _incorrectAnswers.value.toMutableList()
            currentIncorrect.add(IncorrectAnswer(question = q, selectedAnswer = option))
            _incorrectAnswers.value = currentIncorrect
        }
        nextQuestion()
    }

    fun skipQuestion() {
        if (answered.value) return
        times.add( (System.currentTimeMillis() - questionStartTime.value).coerceAtLeast(0L) )
        answered.value = true

        val q = _currentQuestion.value
        val currentIncorrect = _incorrectAnswers.value.toMutableList()
        currentIncorrect.add(IncorrectAnswer(question = q, selectedAnswer = app.getString(R.string.answer_skipped)))
        _incorrectAnswers.value = currentIncorrect

        nextQuestion()
    }

    private fun nextQuestion() {
        val next = _currentIndex.value + 1
        if (next >= _totalQuestions.value) {
            _screen.value = Screen.RESULT
            computeFinalScore() 
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

    fun clearHighScore() {
        sharedPreferences.edit().remove(HIGH_SCORE_KEY).apply()
        _highScore.value = 0.0
    }

    fun computeFinalScore(): Double {
        val total = _totalQuestions.value.coerceAtLeast(1)
        val accuracy = (correctCount.value.toDouble() / total) * 100.0
        val avgTime = if (times.isNotEmpty()) times.average() / 1000.0 else 0.0

        val timeScore: Double
        when {
            avgTime <= 2.0 -> timeScore = 100.0
            avgTime <= 5.0 -> timeScore = ( (5.0 - avgTime) / (5.0 - 2.0) ) * 100.0
            avgTime <= 8.0 -> timeScore = ( ( (8.0 - avgTime) / (8.0 - 5.0) ) * 100.0) - 100.0
            else -> timeScore = -100.0
        }

        val final = (0.75 * accuracy) + (0.25 * (timeScore + 100.0) / 2.0)
        val finalScore = final.coerceIn(0.0, 100.0)

        if (finalScore > _highScore.value) {
            _highScore.value = finalScore
            sharedPreferences.edit().putFloat(HIGH_SCORE_KEY, finalScore.toFloat()).apply()
        }
        return finalScore
    }

    fun summaryText(): String {
        val total = _totalQuestions.value
        val correct = correctCount.value
        val accuracyPercentage = if (total > 0) (correct * 100) / total else 0
        
        val score = computeFinalScore()
        val verdictString = when {
            score >= 80 -> app.getString(R.string.verdict_no_strong_signs)
            score >= 60 -> app.getString(R.string.verdict_mild_signs)
            else -> app.getString(R.string.verdict_significant_signs)
        }

        val accuracySummary = app.getString(R.string.summary_correct_accuracy, correct, total, accuracyPercentage)
        val avgTimeValue = if (times.isNotEmpty()) times.average() / 1000.0 else -1.0 // Use -1 or some indicator for N/A
        val timeSummary = if (avgTimeValue >= 0) String.format(Locale.US, app.getString(R.string.summary_average_time), avgTimeValue) 
                          else app.getString(R.string.summary_average_time_na)
        
        val verdictSummary = app.getString(R.string.summary_verdict_title, verdictString)

        return "$accuracySummary\n$timeSummary\n\n$verdictSummary"
    }

    private fun generateQuestion(): Question {
        val palette = ColorsData.generateTestColors()
        if (palette.isEmpty()) {
            return Question(app.getString(R.string.question_generation_error), Color.Gray, listOf(app.getString(R.string.question_generation_error)))
        }
        val correct = palette.random()
        val namesPool = palette.map { it.name }.toMutableList().apply { remove(correct.name) }
        val options = (listOf(correct.name) + namesPool.shuffled().take(3)).shuffled()
        return Question(correct.name, correct.color, options)
    }
}
