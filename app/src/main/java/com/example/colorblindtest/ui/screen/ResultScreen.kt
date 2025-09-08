package com.example.colorblindtest.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colorblindtest.GameViewModel
import com.example.colorblindtest.R
import com.example.colorblindtest.model.GameMode
import com.example.colorblindtest.model.IncorrectAnswer

@Composable
fun ResultScreen(vm: GameViewModel, onRestart: () -> Unit) {
    val score = vm.computeFinalScore()
    val summary = vm.summaryText()
    val incorrectAnswers by vm.incorrectAnswers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ResultHeader(score)
        }
        item {
            SummaryCard(summary)
        }
        item {
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(stringResource(R.string.result_restart_button), style = MaterialTheme.typography.titleSmall)
            }
        }

        if (incorrectAnswers.isNotEmpty()) {
            item {
                IncorrectAnswersHeader()
            }
            items(incorrectAnswers) { incorrectAnswer ->
                IncorrectAnswerReviewCard(incorrectAnswer = incorrectAnswer)
            }
        }
    }
}

@Composable
private fun ResultHeader(score: Double) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.result_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.result_score, score),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.result_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncorrectAnswersHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            stringResource(R.string.result_review_incorrect_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun IncorrectAnswerReviewCard(incorrectAnswer: IncorrectAnswer) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = incorrectAnswer.question.prompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )

            if (incorrectAnswer.gameMode == GameMode.NORMAL) {
                NormalModeReview(incorrectAnswer)
            } else {
                ReverseModeReview(incorrectAnswer)
            }
        }
    }
}

@Composable
private fun NormalModeReview(incorrectAnswer: IncorrectAnswer) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Top
    ) {
        AnswerItem(
            label = stringResource(id = R.string.result_your_answer_label),
            isCorrect = false
        ) {
            Text(
                text = incorrectAnswer.selectedAnswer as String,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
        AnswerItem(
            label = stringResource(id = R.string.result_correct_answer_label),
            isCorrect = true
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(incorrectAnswer.question.color, RoundedCornerShape(4.dp))
                )
                Text(
                    text = incorrectAnswer.question.correctName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReverseModeReview(incorrectAnswer: IncorrectAnswer) {
    val selectedColor = incorrectAnswer.selectedAnswer as Color
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.Top
    ) {
        AnswerItem(
            label = stringResource(id = R.string.result_your_answer_label),
            isCorrect = false
        ) {
            if (selectedColor == Color.Transparent) {
                Text(
                    text = stringResource(id = R.string.answer_skipped),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(selectedColor, RoundedCornerShape(8.dp))
                )
            }
        }
        AnswerItem(
            label = stringResource(id = R.string.result_correct_answer_label),
            isCorrect = true
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(incorrectAnswer.question.color, RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
private fun AnswerItem(label: String, isCorrect: Boolean, content: @Composable () -> Unit) {
    val color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val icon = if (isCorrect) Icons.Default.Check else Icons.Default.Close

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
    }
}
