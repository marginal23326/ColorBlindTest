package com.example.colorblindtest

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ColorItem(val name: String, val color: Color)

object ColorsData {

    private val baseColors = mapOf(
        // reds: brighter, purer reds
        "Red" to Triple(160..220, 0..50, 0..50),
        // greens: yellowish tint (more red in green channel)
        "Green" to Triple(80..135, 160..255, 0..50),
        // blues: adjusted to be more distinctly blue
        "Blue" to Triple(0..30, 0..80, 200..255),
        // yellows: adjusted to keep R and G closer, G not too far above R
        "Yellow" to Triple(190..255, 190..235, 0..50),
        // oranges: challenging (mix red+yellow)
        "Orange" to Triple(210..255, 130..170, 0..40),
        // purples: adjusted to be more distinct from pink and blue
        "Purple" to Triple(125..150, 0..50, 150..220),
        // browns: slightly darker/muted
        "Brown" to Triple(100..140, 40..75, 10..40),
        // pinks: harder to distinguish from light red
        "Pink" to Triple(220..255, 120..160, 160..200)
    )

    val confusingColors = mapOf(
        "Green" to listOf("Yellow", "Brown"),
        "Red" to listOf("Brown", "Orange"),
        "Purple" to listOf("Blue", "Pink"),
        "Blue" to listOf("Purple"),
        "Yellow" to listOf("Green", "Orange"),
        "Brown" to listOf("Red", "Green", "Orange"),
        "Orange" to listOf("Red", "Yellow", "Brown"),
        "Pink" to listOf("Purple", "Red") // Assuming pink might be confused with light purple or light red
        // Add more relationships as needed
    )

    /** Generate a random shade of the given base color. */
    fun randomColor(name: String): Color {
        val ranges = baseColors[name] ?: error("Unknown color: $name")
        val r = Random.nextInt(ranges.first.first, ranges.first.last + 1)
        val g = Random.nextInt(ranges.second.first, ranges.second.last + 1)
        val b = Random.nextInt(ranges.third.first, ranges.third.last + 1)
        return Color(r, g, b)
    }

    /** Return a ColorItem for each base color, with a random shade. */
    fun generateTestColors(): List<ColorItem> {
        return baseColors.keys.map { name ->
            ColorItem(name, randomColor(name))
        }
    }

    /** Generates a list of 3 distractor color items, trying to pick confusing ones. */
    fun generateDistractorColorItems(correctColorName: String, allColors: List<ColorItem>, count: Int = 3): List<ColorItem> {
        val distractors = mutableListOf<ColorItem>()
        val potentialConfusingNames = confusingColors[correctColorName]?.shuffled() ?: emptyList()

        // Add confusing colors first
        for (confusingName in potentialConfusingNames) {
            if (distractors.size < count) {
                allColors.find { it.name == confusingName && it.name != correctColorName }?.let {
                    distractors.add(it)
                }
            }
        }

        // Add other random colors if not enough confusing ones were found
        val remainingRandomColors = allColors.filter {
            it.name != correctColorName && distractors.none { d -> d.name == it.name }
        }.shuffled()

        var i = 0
        while (distractors.size < count && i < remainingRandomColors.size) {
            distractors.add(remainingRandomColors[i])
            i++
        }
        return distractors.take(count) // Ensure we don't exceed count if remainingRandomColors is small
    }
}
