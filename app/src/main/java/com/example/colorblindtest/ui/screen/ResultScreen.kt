package com.example.colorblindtest.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ResultHeader(score)
        SummaryCard(summary)
        Button(onClick = onRestart) {
            Text(stringResource(R.string.result_restart_button), style = MaterialTheme.typography.bodyMedium)
        }
        if (incorrectAnswers.isNotEmpty()) {
            IncorrectAnswersSection(incorrectAnswers)
        }
    }
}

@Composable
private fun ResultHeader(score: Double) {
    Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.result_score, score), style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun SummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun ColumnScope.IncorrectAnswersSection(incorrectAnswers: List<IncorrectAnswer>) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
    Text(stringResource(R.string.result_review_incorrect_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(incorrectAnswers) { incorrectAnswer ->
            IncorrectAnswerReviewCard(incorrectAnswer = incorrectAnswer)
        }
    }
}

@Composable
private fun IncorrectAnswerReviewCard(incorrectAnswer: IncorrectAnswer) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(incorrectAnswer.question.color, RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.result_your_answer, incorrectAnswer.selectedAnswer as String),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.result_correct_answer, incorrectAnswer.question.correctName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun ReverseModeReview(incorrectAnswer: IncorrectAnswer) {
    Text(
        text = incorrectAnswer.question.prompt,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onErrorContainer
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnswerColorBox(
            label = stringResource(R.string.result_your_answer_color, incorrectAnswer.question.correctName),
            color = incorrectAnswer.selectedAnswer as Color
        )
        Spacer(modifier = Modifier.width(8.dp))
        AnswerColorBox(
            label = stringResource(R.string.result_correct_answer_color, incorrectAnswer.question.correctName),
            color = incorrectAnswer.question.color
        )
    }
    if (incorrectAnswer.selectedAnswer == Color.Transparent) {
        Text(
            text = stringResource(R.string.answer_skipped_color),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowScope.AnswerColorBox(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}
