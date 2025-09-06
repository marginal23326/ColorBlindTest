package com.example.colorblindtest.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colorblindtest.GameViewModel
import com.example.colorblindtest.R
import com.example.colorblindtest.model.GameMode
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(vm: GameViewModel) {
    val q by vm.currentQuestion.collectAsState()
    val index by vm.currentIndex.collectAsState()
    val total by vm.totalQuestions.collectAsState()
    val isAnswered by vm.answered.collectAsState()
    val gameMode by vm.gameMode.collectAsState()

    // Collect feedback states
    val showFeedback by vm.showFeedback.collectAsState()
    val wasCorrectDisplay by vm.wasCorrectDisplay.collectAsState()
    val correctOptionForDisplay by vm.correctOptionForDisplay.collectAsState()
    val userSelectedOption by vm.selectedAnswer.collectAsState() // The answer user picked

    var elapsedDisplay by remember { mutableLongStateOf(0L) }

    // Colors for feedback
    val correctAnswerFeedbackColor = Color(0xFF388E3C) // Dark Green
    val incorrectAnswerFeedbackColor = Color(0xFFD32F2F) // Dark Red
    val correctAnswerContainerColor = Color(0xFFC8E6C9) // Light Green for container
    val incorrectAnswerContainerColor = Color(0xFFFFCDD2) // Light Red for container
    val correctBorder = BorderStroke(3.dp, correctAnswerFeedbackColor)
    val incorrectBorder = BorderStroke(3.dp, incorrectAnswerFeedbackColor)


    LaunchedEffect(q, isAnswered, showFeedback) {
        if (!isAnswered && !showFeedback) {
            vm.markQuestionStart()
            while (!vm.answered.value && !vm.showFeedback.value) {
                elapsedDisplay = ((System.currentTimeMillis() - vm.questionStartTime.value) / 1000)
                delay(150)
            }
        } else if (isAnswered && !showFeedback) {
            elapsedDisplay = ((System.currentTimeMillis() - vm.questionStartTime.value) / 1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { vm.resetGame() }) {
                        Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.game_quit_button))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply innerPadding from Scaffold
                .padding(horizontal = 16.dp, vertical = 16.dp), // Existing padding
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: progress and time
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.game_question_progress, index + 1, total), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.game_time_elapsed, elapsedDisplay), style = MaterialTheme.typography.titleMedium)
            }
            LinearProgressIndicator(
                progress = { ((index + 1).coerceAtMost(total)).toFloat() / total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth()
            )

            // Question display area
            if (gameMode == GameMode.NORMAL) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(q.color),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            } else { // REVERSE mode
                Text(
                    text = q.prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                )
            }

            // Options
            if (gameMode == GameMode.NORMAL) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    q.options.forEach { optAny ->
                        val opt = optAny as String

                        val actualButtonEnabledState = if (showFeedback) true else !isAnswered
                        val canExecuteClick = !showFeedback && !isAnswered

                        val buttonColors = if (showFeedback) {
                            if (opt == userSelectedOption) { // This button is the one the user selected
                                if (wasCorrectDisplay == true) { // And it was CORRECT
                                    ButtonDefaults.buttonColors(
                                        containerColor = correctAnswerContainerColor,
                                        contentColor = correctAnswerFeedbackColor
                                    )
                                } else { // And it was INCORRECT
                                    ButtonDefaults.buttonColors(
                                        containerColor = incorrectAnswerContainerColor,
                                        contentColor = incorrectAnswerFeedbackColor
                                    )
                                }
                            } else if (opt == correctOptionForDisplay && wasCorrectDisplay == false) { // This button is the ACTUAL CORRECT answer, and the user picked something else
                                ButtonDefaults.buttonColors(
                                    containerColor = correctAnswerContainerColor,
                                    contentColor = correctAnswerFeedbackColor
                                )
                            } else { // Other buttons during feedback (muted)
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            // Not in feedback mode, standard tonal button
                            ButtonDefaults.filledTonalButtonColors()
                        }

                        Button(
                            onClick = {
                                if (canExecuteClick) {
                                    vm.submitAnswer(opt)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = actualButtonEnabledState, // Button is '''render-enabled''' during feedback for colors
                            colors = buttonColors
                        ) {
                            Text(opt, modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else { // REVERSE mode - Color options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    q.options.forEach { optAny ->
                        val colorOption = optAny as Color

                        var currentBorder: BorderStroke? = null
                        var currentElevation = CardDefaults.cardElevation(defaultElevation = 2.dp)

                        if (showFeedback) {
                            if (colorOption == correctOptionForDisplay && wasCorrectDisplay == true) { // User picked correct
                                currentBorder = correctBorder
                                currentElevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            } else if (colorOption == correctOptionForDisplay && wasCorrectDisplay == false) { // This is the correct one, user picked wrong
                                currentBorder = correctBorder
                                currentElevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Slightly elevated
                            } else if (colorOption == userSelectedOption && wasCorrectDisplay == false) { // User picked this, and it'''s wrong
                                currentBorder = incorrectBorder
                                currentElevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            } else {
                                // Other options during feedback, keep them plain
                                currentElevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            }
                        }
                        val currentCardClickableEnabled = if (showFeedback) false else !isAnswered

                        Card(
                            modifier = Modifier
                                .size(72.dp)
                                .then(if (currentBorder != null) Modifier.border(currentBorder, RoundedCornerShape(12.dp)) else Modifier)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = currentCardClickableEnabled) { vm.submitAnswer(colorOption) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = currentElevation
                        ) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .background(colorOption))
                        }
                    }
                }
            }

            val correct by vm.correctCount.collectAsState()
            Spacer(modifier = Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.game_correct_count, correct), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    if (!showFeedback && !isAnswered) {
                        vm.skipQuestion()
                    }
                }, enabled = if (showFeedback) true else !isAnswered) { // Keep enabled for consistent look, control via onClick
                    Text(stringResource(R.string.game_skip_button), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
