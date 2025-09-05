package com.example.colorblindtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colorblindtest.ui.theme.ColorBlindTestTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val vm by viewModels<GameViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ColorBlindTestTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppHost(vm = vm)
                }
            }
        }
    }
}

@Composable
fun AppHost(vm: GameViewModel) {
    val screen by vm.screen.collectAsState()
    when (screen) {
        Screen.HOME -> HomeScreen(vm = vm, onStart = { vm.startGame() })
        Screen.GAME -> GameScreen(vm = vm)
        Screen.RESULT -> ResultScreen(vm = vm, onRestart = { vm.resetGame() })
    }
}

/* =========================
   HOME SCREEN – Polished
   ========================= */
@Composable
fun HomeScreen(vm: GameViewModel, onStart: () -> Unit) {
    val currentTotalQuestions by vm.totalQuestions.collectAsState()
    val highScore by vm.highScore.collectAsState()
    val questionOptions = listOf(5, 10, 15, 20)

    androidx.compose.material3.Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStart,
                icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                text = { Text(stringResource(R.string.home_start_button, currentTotalQuestions)) }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // High score card (only when available)
            if (highScore > 0) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_high_score, highScore),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.summary_average_time_na), // Placeholder subline – can be replaced if you expose avg time on home later
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        TextButton(onClick = { vm.clearHighScore() }) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(text = stringResource(R.string.home_clear_high_score_button))
                        }
                    }
                }
            }

            // Instructions card
            InstructionCard(
                instructions = stringResource(R.string.home_instructions)
            )

            // Question selection section
            QuestionSelectionSection(
                current = currentTotalQuestions,
                options = questionOptions,
                onSelect = { vm.setTotalQuestions(it) }
            )

            // Spacer to keep content above the FAB on small screens
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun InstructionCard(instructions: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = instructions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuestionSelectionSection(
    current: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.home_number_of_questions, current),
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(options) { count ->
                val selected = current == count
                FilterChip(
                    selected = selected,
                    onClick = { onSelect(count) },
                    label = {
                        Text(
                            text = "$count",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null
                            )
                        }
                    } else null,
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

/* =========================
   GAME SCREEN – Polished
   ========================= */
@Composable
fun GameScreen(vm: GameViewModel) {
    val q by vm.currentQuestion.collectAsState()
    val index by vm.currentIndex.collectAsState()
    val total by vm.totalQuestions.collectAsState()
    val isAnswered by vm.answered.collectAsState()

    var elapsedDisplay by remember { mutableLongStateOf(0L) }
    LaunchedEffect(q, isAnswered) {
        if (!isAnswered) {
            vm.markQuestionStart()
            while (!vm.answered.value) {
                elapsedDisplay = ((System.currentTimeMillis() - vm.questionStartTime.value) / 1000)
                delay(150)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header: progress and time
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.game_question_progress, index + 1, total),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.game_time_elapsed, elapsedDisplay),
                style = MaterialTheme.typography.titleMedium
            )
        }
        LinearProgressIndicator(
            progress = ((index + 1).coerceAtMost(total)).toFloat() / total.coerceAtLeast(1),
            modifier = Modifier.fillMaxWidth()
        )

        // Color patch card
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

        // Options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            q.options.forEach { opt ->
                Button(
                    onClick = { vm.submitAnswer(opt) },
                    modifier = Modifier
                        .fillMaxWidth(),
                    enabled = !isAnswered,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Text(
                        opt,
                        modifier = Modifier.padding(vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        val correct by vm.correctCount.collectAsState()
        Spacer(modifier = Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.game_correct_count, correct),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(onClick = { vm.skipQuestion() }, enabled = !isAnswered) {
                Text(
                    text = stringResource(R.string.game_skip_button),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/* =========================
   RESULT SCREEN – Polished
   ========================= */
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
        Text(
            text = stringResource(R.string.result_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.result_score, score),
            style = MaterialTheme.typography.titleMedium
        )

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }

        Button(onClick = onRestart) {
            Text(
                text = stringResource(R.string.result_restart_button),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (incorrectAnswers.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                text = stringResource(R.string.result_review_incorrect_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(incorrectAnswers) { incorrectAnswer ->
                    ElevatedCard(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(incorrectAnswer.question.color, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.result_your_answer, incorrectAnswer.selectedAnswer),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.result_correct_answer, incorrectAnswer.question.correctName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}