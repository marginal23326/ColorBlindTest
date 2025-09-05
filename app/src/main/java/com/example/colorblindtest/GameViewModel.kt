package com.example.colorblindtest

import android.app.Application
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import androidx.core.content.edit

enum class Screen { HOME, GAME, RESULT }
enum class GameMode { NORMAL, REVERSE }

data class Question(
    val prompt: String, // For REVERSE mode: "Which is Red?"; For NORMAL: Color patch is the prompt.
    val correctName: String, // Name of the correct color
    val color: Color, // Correct color. In NORMAL mode, this is the color displayed. In REVERSE, this is one of the options.
    val options: List<Any> // List<String> for NORMAL, List<Color> for REVERSE
)

data class IncorrectAnswer(
    val question: Question,
    val selectedAnswer: Any, // String for NORMAL, Color for REVERSE
    val gameMode: GameMode // To help display review correctly
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        const val DEFAULT_TOTAL_QUESTIONS = 10
        private const val PREFS_NAME = "ColorBlindTestPrefs"
        private const val HIGH_SCORE_KEY = "highScore" // Consider separate high scores for different modes
        private const val GAME_MODE_KEY = "gameMode"
    }

    private val app = getApplication<Application>()
    private val sharedPreferences = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _screen = MutableStateFlow(Screen.HOME)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.NORMAL)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _totalQuestions = MutableStateFlow(DEFAULT_TOTAL_QUESTIONS)
    val totalQuestions: StateFlow<Int> = _totalQuestions

    private val _highScore = MutableStateFlow(0.0)
    val highScore: StateFlow<Double> = _highScore.asStateFlow()

    private val _currentQuestion = MutableStateFlow(generateQuestion())
    val currentQuestion: StateFlow<Question> = _currentQuestion.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    val correctCount = MutableStateFlow(0)
    val selectedAnswer = MutableStateFlow<Any?>(null) // Can be String (color name) or Color object
    val answered = MutableStateFlow(false)

    private val _incorrectAnswers = MutableStateFlow<List<IncorrectAnswer>>(emptyList())
    val incorrectAnswers: StateFlow<List<IncorrectAnswer>> = _incorrectAnswers.asStateFlow()

    val questionStartTime = MutableStateFlow(0L)
    private val times = mutableListOf<Long>()

    init {
        _highScore.value = sharedPreferences.getFloat(HIGH_SCORE_KEY, 0f).toDouble()
        val savedMode = sharedPreferences.getString(GAME_MODE_KEY, GameMode.NORMAL.name)
        _gameMode.value = GameMode.valueOf(savedMode ?: GameMode.NORMAL.name)
    }

    fun setGameMode(mode: GameMode) {
        _gameMode.value = mode
        sharedPreferences.edit { putString(GAME_MODE_KEY, mode.name) }
        // Potentially reset or update game if mode changes mid-state, though usually set from home
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
        _currentQuestion.value = generateQuestion() // Generates based on current _gameMode
        _screen.value = Screen.GAME
        selectedAnswer.value = null
        answered.value = false
    }

    fun markQuestionStart() {
        questionStartTime.value = System.currentTimeMillis()
    }

    fun submitAnswer(option: Any) {
        if (answered.value) return
        val elapsed = System.currentTimeMillis() - questionStartTime.value
        times.add(elapsed)
        selectedAnswer.value = option
        answered.value = true

        val q = _currentQuestion.value
        var isCorrect: Boolean
        when (_gameMode.value) {
            GameMode.NORMAL -> {
                isCorrect = (option as? String) == q.correctName
            }
            GameMode.REVERSE -> {
                // In REVERSE mode, q.color is the correct Color object,
                // and option is the selected Color object.
                isCorrect = (option as? Color) == q.color
            }
        }

        if (isCorrect) {
            correctCount.value = correctCount.value + 1
        } else {
            val currentIncorrect = _incorrectAnswers.value.toMutableList()
            currentIncorrect.add(IncorrectAnswer(question = q, selectedAnswer = option, gameMode = _gameMode.value))
            _incorrectAnswers.value = currentIncorrect
        }
        nextQuestion()
    }

    fun skipQuestion() {
        if (answered.value) return
        times.add( (System.currentTimeMillis() - questionStartTime.value).coerceAtLeast(0L) )
        answered.value = true

        val q = _currentQuestion.value
        val skippedAnswerRepresentation: Any = when (_gameMode.value) {
            GameMode.NORMAL -> app.getString(R.string.answer_skipped)
            GameMode.REVERSE -> Color.Transparent // Or some other placeholder for a skipped color
        }
        val currentIncorrect = _incorrectAnswers.value.toMutableList()
        currentIncorrect.add(IncorrectAnswer(question = q, selectedAnswer = skippedAnswerRepresentation, gameMode = _gameMode.value))
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
        // Consider if total questions or mode should reset to default here,
        // or if they persist from last settings. Currently, they persist.
    }

    fun clearHighScore() {
        sharedPreferences.edit { remove(HIGH_SCORE_KEY) }
        _highScore.value = 0.0
    }

    fun computeFinalScore(): Double {
        val total = _totalQuestions.value.coerceAtLeast(1)
        val accuracy = (correctCount.value.toDouble() / total) * 100.0
        val avgTime = if (times.isNotEmpty()) times.average() / 1000.0 else 0.0

        // Scoring logic might need to be adjusted or made separate for different modes if desired
        val timeScore: Double = when {
            avgTime <= 2.0 -> 100.0 // Perfect time
            avgTime <= 5.0 -> ((5.0 - avgTime) / (5.0 - 2.0)) * 100.0 // Linear decay from 100 to 0
            avgTime <= 8.0 -> (((8.0 - avgTime) / (8.0 - 5.0)) * 100.0) - 100.0 // Linear decay from 0 to -100
            else -> -100.0 // Worst time
        }

        // Weighted average: 75% accuracy, 25% time (normalized timeScore from -100..100 to 0..100)
        val final = (0.75 * accuracy) + (0.25 * (timeScore + 100.0) / 2.0)
        val finalScore = final.coerceIn(0.0, 100.0)


        if (finalScore > _highScore.value) {
            _highScore.value = finalScore
            sharedPreferences.edit { putFloat(HIGH_SCORE_KEY, finalScore.toFloat()) }
        }
        return finalScore
    }

    fun summaryText(): String {
        val total = _totalQuestions.value
        val correct = correctCount.value
        val accuracyPercentage = if (total > 0) (correct * 100) / total else 0

        val score = computeFinalScore()
        // Verdict might need to be mode-specific if the interpretation of "color confusion" changes
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
        val modeInfo = app.getString(if (_gameMode.value == GameMode.NORMAL) R.string.mode_normal else R.string.mode_reverse)


        return "$modeInfo\n$accuracySummary\n$timeSummary\n\n$verdictSummary"
    }

    private fun generateQuestion(): Question {
        val allColorItems = ColorsData.generateTestColors() // List<ColorItem>
        if (allColorItems.isEmpty()) {
            // Fallback for error
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
                    prompt = "", // Color patch itself is the prompt
                    correctName = correctColorItem.name,
                    color = correctColorItem.color, // This is the color to be displayed
                    options = nameOptions // List<String>
                )
            }
            GameMode.REVERSE -> {
                val correctColorItem = allColorItems.random() // This is the color to be identified by name
                // Generate 3 distractor colors, prioritizing confusing ones
                val distractorColorItems = ColorsData.generateDistractorColorItems(
                    correctColorName = correctColorItem.name,
                    allColors = allColorItems,
                    count = 3
                )
                val colorOptions = (listOf(correctColorItem.color) + distractorColorItems.map { it.color }).shuffled()

                Question(
                    prompt = app.getString(R.string.question_prompt_reverse, correctColorItem.name),
                    correctName = correctColorItem.name, // Name of the color to find
                    color = correctColorItem.color,   // The actual Color object that is correct
                    options = colorOptions // List<Color>
                )
            }
        }
    }
}
