package com.example.colorblindtest

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ColorItem(val name: String, val color: Color)

object ColorsData {

    private val baseColors = mapOf(
        // reds: darker to make them look more brown for protanopia
        "Red" to Triple(120..200, 0..60, 0..60),
        // greens: yellowish tint (more red in green channel)
        "Green" to Triple(80..160, 160..255, 0..80),
        // blues: adjusted to be more distinctly blue
        "Blue" to Triple(0..30, 0..80, 200..255),
        // yellows: adjusted to keep R and G closer, G not too far above R
        "Yellow" to Triple(190..255, 190..235, 0..50),
        // oranges: challenging (mix red+yellow)
        "Orange" to Triple(200..255, 100..180, 0..60),
        // purples: adjusted to be more distinct from pink and blue
        "Purple" to Triple(100..150, 0..50, 150..220),
        // browns: slightly darker/muted
        "Brown" to Triple(80..140, 40..90, 0..50),
        // pinks: harder to distinguish from light red
        "Pink" to Triple(220..255, 100..160, 150..200)
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
}
