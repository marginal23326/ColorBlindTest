package com.example.colorblindtest

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.colorblindtest.model.Difficulty
import com.example.colorblindtest.model.GameMode
import com.example.colorblindtest.model.IncorrectAnswer
import com.example.colorblindtest.model.Question
import com.example.colorblindtest.model.Screen
import java.util.Locale
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class GameViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val DEFAULT_TOTAL_QUESTIONS = 10
        private const val PREFS_NAME = "ColorBlindTestPrefs"
        private const val HIGH_SCORE_KEY = "highScore"
        private const val HIGH_SCORE_AVG_TIME_KEY = "highScoreAverageTime"
        private const val GAME_MODE_KEY = "gameMode"
        private const val DIFFICULTY_KEY = "difficulty"
        private const val FEEDBACK_DURATION_MS_CORRECT = 500L
        private const val FEEDBACK_DURATION_MS_INCORRECT = 1000L

        // Scoring constants
        private const val ACCURACY_WEIGHT = 0.75
        private const val TIME_WEIGHT = 0.25
        private const val FAST_TIME_THRESHOLD = 2.0
        private const val MEDIUM_TIME_THRESHOLD = 5.0
        private const val SLOW_TIME_THRESHOLD = 8.0
    }

    private val app = getApplication<Application>()
    private val sharedPreferences = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.NORMAL)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _difficulty = MutableStateFlow(Difficulty.MEDIUM)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _totalQuestions = MutableStateFlow(DEFAULT_TOTAL_QUESTIONS)
    val totalQuestions: StateFlow<Int> = _totalQuestions

    private val _highScore = MutableStateFlow(0.0)
    val highScore: StateFlow<Double> = _highScore.asStateFlow()

    private val _highScoreAverageTime = MutableStateFlow(-1.0f)
    val highScoreAverageTime: StateFlow<Float> = _highScoreAverageTime.asStateFlow()

    private val _currentQuestion = MutableStateFlow(generateQuestion())
    val currentQuestion: StateFlow<Question> = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val correctCount = MutableStateFlow(0)
    val selectedAnswer = MutableStateFlow<Any?>(null)
    val answered = MutableStateFlow(false) // True when an answer is submitted, false when new q loads or during feedback delay

    private val _incorrectAnswers = MutableStateFlow<List<IncorrectAnswer>>(emptyList())
    val incorrectAnswers: StateFlow<List<IncorrectAnswer>> = _incorrectAnswers.asStateFlow()

    val questionStartTime = MutableStateFlow(0L)
    private val times = mutableListOf<Long>()

    // Feedback states
    private val _showFeedback = MutableStateFlow(false)
    val showFeedback: StateFlow<Boolean> = _showFeedback.asStateFlow()

    private val _wasCorrectDisplay = MutableStateFlow<Boolean?>(null) // true for correct, false for incorrect, null otherwise
    val wasCorrectDisplay: StateFlow<Boolean?> = _wasCorrectDisplay.asStateFlow()

    private val _correctOptionForDisplay = MutableStateFlow<Any?>(null) // Stores the actual correct option to highlight
    val correctOptionForDisplay: StateFlow<Any?> = _correctOptionForDisplay.asStateFlow()


    init {
        _highScore.value = sharedPreferences.getFloat(HIGH_SCORE_KEY, 0f).toDouble()
        _highScoreAverageTime.value = sharedPreferences.getFloat(HIGH_SCORE_AVG_TIME_KEY, -1.0f)
        val savedMode = sharedPreferences.getString(GAME_MODE_KEY, GameMode.NORMAL.name)
        _gameMode.value = GameMode.valueOf(savedMode ?: GameMode.NORMAL.name)
        val savedDifficulty = sharedPreferences.getString(DIFFICULTY_KEY, Difficulty.MEDIUM.name)
        _difficulty.value = Difficulty.valueOf(savedDifficulty ?: Difficulty.MEDIUM.name)
    }

    fun setGameMode(mode: GameMode) {
        _gameMode.value = mode
        sharedPreferences.edit { putString(GAME_MODE_KEY, mode.name) }
    }

    fun setDifficulty(difficulty: Difficulty) {
        _difficulty.value = difficulty
        sharedPreferences.edit { putString(DIFFICULTY_KEY, difficulty.name) }
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
        selectedAnswer.value = null
        answered.value = false
        _showFeedback.value = false
        _wasCorrectDisplay.value = null
        _correctOptionForDisplay.value = null
        _currentQuestion.value = generateQuestion()
        _screen.value = Screen.GAME
    }

    fun markQuestionStart() {
        questionStartTime.value = System.currentTimeMillis()
    }

    fun submitAnswer(option: Any) {
        if (answered.value || _showFeedback.value) return
        val isCorrect = when (_gameMode.value) {
            GameMode.NORMAL -> (option as? String) == _currentQuestion.value.correctName
            GameMode.REVERSE, GameMode.SHADE -> (option as? Color) == _currentQuestion.value.color
        }
        handleAnswer(option, isCorrect)
    }

    fun skipQuestion() {
        if (answered.value || _showFeedback.value) return
        val skippedAnswer = when (_gameMode.value) {
            GameMode.NORMAL -> app.getString(R.string.answer_skipped)
            GameMode.REVERSE, GameMode.SHADE -> Color.Transparent
        }
        handleAnswer(skippedAnswer, isCorrect = false)
    }

    private fun handleAnswer(selectedOption: Any, isCorrect: Boolean) {
        val elapsed = System.currentTimeMillis() - questionStartTime.value
        times.add(elapsed)
        selectedAnswer.value = selectedOption
        answered.value = true

        val q = _currentQuestion.value
        _correctOptionForDisplay.value = when (_gameMode.value) {
            GameMode.NORMAL -> q.correctName
            GameMode.REVERSE, GameMode.SHADE -> q.color
        }
        _wasCorrectDisplay.value = isCorrect

        if (isCorrect) {
            correctCount.value++
        } else {
            _incorrectAnswers.value = _incorrectAnswers.value + IncorrectAnswer(
                question = q,
                selectedAnswer = selectedOption,
                gameMode = _gameMode.value
            )
        }

        _showFeedback.value = true
        viewModelScope.launch {
            val feedbackDuration = if (isCorrect) FEEDBACK_DURATION_MS_CORRECT else FEEDBACK_DURATION_MS_INCORRECT
            delay(feedbackDuration)
            _showFeedback.value = false
            _wasCorrectDisplay.value = null
            _correctOptionForDisplay.value = null
            nextQuestion()
        }
    }

    private fun nextQuestion() {
        val next = _currentIndex.value + 1
        if (next >= _totalQuestions.value) {
            _screen.value = Screen.RESULT
            computeFinalScore()
        } else {
            _currentIndex.value = next
            _currentQuestion.value = generateQuestion()
            selectedAnswer.value = null // Crucial to reset for the new question
            answered.value = false     // Allow answering for the new question
        }
        // Feedback states are reset by the coroutine before calling nextQuestion
    }

    fun resetGame() {
        _screen.value = Screen.HOME
        // Reset feedback states for a fresh start from home
        _showFeedback.value = false
        _wasCorrectDisplay.value = null
        _correctOptionForDisplay.value = null
    }

    fun clearHighScore() {
        sharedPreferences.edit {
            remove(HIGH_SCORE_KEY)
            remove(HIGH_SCORE_AVG_TIME_KEY)
        }
        _highScore.value = 0.0
        _highScoreAverageTime.value = -1.0f
    }

    fun computeFinalScore(): Double {
        val total = _totalQuestions.value.coerceAtLeast(1)
        val accuracy = (correctCount.value.toDouble() / total) * 100.0
        val avgTime = if (times.isNotEmpty()) times.average() / 1000.0 else 0.0

        val timeScore: Double = when {
            avgTime <= FAST_TIME_THRESHOLD -> 100.0
            avgTime <= MEDIUM_TIME_THRESHOLD -> ((MEDIUM_TIME_THRESHOLD - avgTime) / (MEDIUM_TIME_THRESHOLD - FAST_TIME_THRESHOLD)) * 100.0
            avgTime <= SLOW_TIME_THRESHOLD -> (((SLOW_TIME_THRESHOLD - avgTime) / (SLOW_TIME_THRESHOLD - MEDIUM_TIME_THRESHOLD)) * 100.0) - 100.0
            else -> -100.0
        }

        val final = (ACCURACY_WEIGHT * accuracy) + (TIME_WEIGHT * (timeScore + 100.0) / 2.0)
        val finalScore = final.coerceIn(0.0, 100.0)

        if (finalScore > _highScore.value) {
            _highScore.value = finalScore
            _highScoreAverageTime.value = avgTime.toFloat()
            sharedPreferences.edit {
                putFloat(HIGH_SCORE_KEY, finalScore.toFloat())
                putFloat(HIGH_SCORE_AVG_TIME_KEY, avgTime.toFloat())
            }
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
        val avgTimeValue = if (times.isNotEmpty()) times.average() / 1000.0 else -1.0
        val timeSummary = if (avgTimeValue >= 0) String.format(Locale.US, app.getString(R.string.summary_average_time), avgTimeValue)
        else app.getString(R.string.summary_average_time_na)

        val verdictSummary = app.getString(R.string.summary_verdict_title, verdictString)
        val modeInfo = app.getString(
            when (_gameMode.value) {
                GameMode.NORMAL -> R.string.mode_normal
                GameMode.REVERSE -> R.string.mode_reverse
                GameMode.SHADE -> R.string.mode_shade
            }
        )

        return "$modeInfo\n$accuracySummary\n$timeSummary\n\n$verdictSummary"
    }

    private fun generateQuestion(): Question {
        val allColorItems = ColorsData.generateTestColors()
        if (allColorItems.isEmpty()) {
            return Question(
                prompt = app.getString(R.string.question_generation_error),
                correctName = "Error",
                color = Color.Gray,
                options = listOf(app.getString(R.string.question_generation_error))
            )
        }

        return when (_gameMode.value) {
            GameMode.NORMAL -> {
                val correctColorItem = allColorItems.random()
                val otherColorNames = allColorItems.map { it.name }
                    .filter { it != correctColorItem.name }
                    .shuffled()
                    .take(3)
                val nameOptions = (listOf(correctColorItem.name) + otherColorNames).shuffled()
                Question(
                    prompt = "",
                    correctName = correctColorItem.name,
                    color = correctColorItem.color,
                    options = nameOptions
                )
            }
            GameMode.REVERSE -> {
                val correctColorItem = allColorItems.random()
                val distractorColorItems = ColorsData.generateDistractorColorItems(
                    correctColorName = correctColorItem.name,
                    allColors = allColorItems,
                    count = 3
                )
                val colorOptions = (listOf(correctColorItem.color) + distractorColorItems.map { it.color }).shuffled()
                Question(
                    prompt = String.format(app.getString(R.string.question_prompt_reverse), correctColorItem.name),
                    correctName = correctColorItem.name,
                    color = correctColorItem.color,
                    options = colorOptions
                )
            }
            GameMode.SHADE -> {
                val (options, correctColor) = ColorsData.generateShadeQuestionOptions(9, _difficulty.value)
                Question(
                    prompt = app.getString(R.string.question_prompt_shade),
                    correctName = "", // Not used in this mode
                    color = correctColor,
                    options = options
                )
            }
        }
    }
}
