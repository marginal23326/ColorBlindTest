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
import com.example.colorblindtest.model.Question
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(vm: GameViewModel) {
    val q by vm.currentQuestion.collectAsState()
    val index by vm.currentIndex.collectAsState()
    val total by vm.totalQuestions.collectAsState()
    val isAnswered by vm.answered.collectAsState()
    val gameMode by vm.gameMode.collectAsState()
    val showFeedback by vm.showFeedback.collectAsState()
    val correctCount by vm.correctCount.collectAsState()

    var elapsedDisplay by remember { mutableLongStateOf(0L) }

    LaunchedEffect(q, isAnswered, showFeedback) {
        if (!isAnswered && !showFeedback) {
            vm.markQuestionStart()
            while (!vm.answered.value && !vm.showFeedback.value) {
                elapsedDisplay = (System.currentTimeMillis() - vm.questionStartTime.value) / 1000
                delay(150)
            }
        } else if (isAnswered && !showFeedback) {
            elapsedDisplay = (System.currentTimeMillis() - vm.questionStartTime.value) / 1000
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
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GameHeader(index, total, elapsedDisplay)

            if (gameMode == GameMode.NORMAL) {
                NormalModeUI(vm = vm, q = q)
            } else {
                ReverseModeUI(vm = vm, q = q)
            }

            Spacer(modifier = Modifier.weight(1f))

            GameFooter(correctCount, showFeedback, isAnswered) {
                vm.skipQuestion()
            }
        }
    }
}

@Composable
private fun GameHeader(index: Int, total: Int, elapsedDisplay: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            stringResource(R.string.game_question_progress, index + 1, total),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.game_time_elapsed, elapsedDisplay),
            style = MaterialTheme.typography.titleMedium
        )
    }
    LinearProgressIndicator(
        progress = { (index + 1).toFloat() / total.coerceAtLeast(1) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NormalModeUI(vm: GameViewModel, q: Question) {
    val showFeedback by vm.showFeedback.collectAsState()
    val isAnswered by vm.answered.collectAsState()
    val wasCorrectDisplay by vm.wasCorrectDisplay.collectAsState()
    val correctOptionForDisplay by vm.correctOptionForDisplay.collectAsState()
    val userSelectedOption by vm.selectedAnswer.collectAsState()

    val correctAnswerFeedbackColor = Color(0xFF388E3C)
    val incorrectAnswerFeedbackColor = Color(0xFFD32F2F)
    val correctAnswerContainerColor = Color(0xFFC8E6C9)
    val incorrectAnswerContainerColor = Color(0xFFFFCDD2)

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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.options.forEach { optAny ->
            val opt = optAny as String
            val buttonColors = if (showFeedback) {
                when {
                    opt == userSelectedOption && wasCorrectDisplay == true -> ButtonDefaults.buttonColors(
                        containerColor = correctAnswerContainerColor,
                        contentColor = correctAnswerFeedbackColor
                    )
                    opt == userSelectedOption && wasCorrectDisplay == false -> ButtonDefaults.buttonColors(
                        containerColor = incorrectAnswerContainerColor,
                        contentColor = incorrectAnswerFeedbackColor
                    )
                    opt == correctOptionForDisplay && wasCorrectDisplay == false -> ButtonDefaults.buttonColors(
                        containerColor = correctAnswerContainerColor,
                        contentColor = correctAnswerFeedbackColor
                    )
                    else -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                ButtonDefaults.filledTonalButtonColors()
            }

            Button(
                onClick = { if (!showFeedback && !isAnswered) vm.submitAnswer(opt) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAnswered || showFeedback,
                colors = buttonColors
            ) {
                Text(opt, modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ReverseModeUI(vm: GameViewModel, q: Question) {
    val showFeedback by vm.showFeedback.collectAsState()
    val isAnswered by vm.answered.collectAsState()
    val wasCorrectDisplay by vm.wasCorrectDisplay.collectAsState()
    val correctOptionForDisplay by vm.correctOptionForDisplay.collectAsState()
    val userSelectedOption by vm.selectedAnswer.collectAsState()

    val correctAnswerFeedbackColor = Color(0xFF388E3C)
    val incorrectAnswerFeedbackColor = Color(0xFFD32F2F)
    val correctBorder = BorderStroke(3.dp, correctAnswerFeedbackColor)
    val incorrectBorder = BorderStroke(3.dp, incorrectAnswerFeedbackColor)

    Text(
        text = q.prompt,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        q.options.forEach { optAny ->
            val colorOption = optAny as Color
            val (border, elevation) = when {
                showFeedback && colorOption == correctOptionForDisplay && wasCorrectDisplay == true ->
                    correctBorder to CardDefaults.cardElevation(defaultElevation = 8.dp)
                showFeedback && colorOption == correctOptionForDisplay && wasCorrectDisplay == false ->
                    correctBorder to CardDefaults.cardElevation(defaultElevation = 4.dp)
                showFeedback && colorOption == userSelectedOption && wasCorrectDisplay == false ->
                    incorrectBorder to CardDefaults.cardElevation(defaultElevation = 8.dp)
                showFeedback ->
                    null to CardDefaults.cardElevation(defaultElevation = 1.dp)
                else ->
                    null to CardDefaults.cardElevation(defaultElevation = 2.dp)
            }

            Card(
                modifier = Modifier
                    .size(72.dp)
                    .then(if (border != null) Modifier.border(border, RoundedCornerShape(12.dp)) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !showFeedback && !isAnswered) { vm.submitAnswer(colorOption) },
                shape = RoundedCornerShape(12.dp),
                elevation = elevation
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorOption)
                )
            }
        }
    }
}

@Composable
private fun GameFooter(
    correctCount: Int,
    showFeedback: Boolean,
    isAnswered: Boolean,
    onSkip: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.game_correct_count, correctCount),
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(
            onClick = { if (!showFeedback && !isAnswered) onSkip() },
            enabled = !isAnswered || showFeedback
        ) {
            Text(stringResource(R.string.game_skip_button), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
