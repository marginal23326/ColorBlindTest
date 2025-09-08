package com.example.colorblindtest.model

import androidx.compose.ui.graphics.Color

enum class Screen { HOME, GAME, RESULT }
enum class GameMode { NORMAL, REVERSE, SHADE }
enum class Difficulty { MEDIUM, HARD }

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

data class ColorItem(val name: String, val color: Color)
