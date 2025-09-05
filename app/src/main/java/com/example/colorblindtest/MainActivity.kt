package com.example.colorblindtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle // Added for selected game mode
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
import androidx.compose.ui.draw.clip // Added for clickable color patches
import androidx.compose.ui.graphics.Color // Added for type casting
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colorblindtest.ui.theme.ColorBlindTestTheme
import kotlinx.coroutines.delay
import java.util.Locale

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

/* ============
   HOME SCREEN
   ============ */
@Composable
fun HomeScreen(vm: GameViewModel, onStart: () -> Unit) {
    val currentTotalQuestions by vm.totalQuestions.collectAsState()
    val highScore by vm.highScore.collectAsState()
    val highScoreAverageTime by vm.highScoreAverageTime.collectAsState() // Collect average time
    val questionOptions = listOf(5, 10, 15, 20)
    val currentGameMode by vm.gameMode.collectAsState()

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
            verticalArrangement = Arrangement.spacedBy(16.dp) // Adjusted spacing
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (highScore > 0) {
                HighScoreCard(
                    highScore = highScore,
                    averageTime = highScoreAverageTime, // Pass average time
                    onClear = { vm.clearHighScore() }
                )
            }

            InstructionCard(instructions = stringResource(R.string.home_instructions))

            GameModeSelectionSection(
                currentMode = currentGameMode,
                onModeSelect = { vm.setGameMode(it) }
            )

            QuestionSelectionSection(
                current = currentTotalQuestions,
                options = questionOptions,
                onSelect = { vm.setTotalQuestions(it) }
            )

            Spacer(modifier = Modifier.height(72.dp)) // Spacer for FAB
        }
    }
}

@Composable
private fun HighScoreCard(highScore: Double, averageTime: Float, onClear: () -> Unit) { // Added averageTime parameter
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
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
                    text = if (averageTime >= 0) String.format(Locale.US, stringResource(R.string.summary_average_time), averageTime)
                           else stringResource(R.string.summary_average_time_na),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
            TextButton(onClick = onClear) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                Spacer(modifier = Modifier.size(6.dp))
                Text(text = stringResource(R.string.home_clear_high_score_button))
            }
        }
    }
}

@Composable
private fun InstructionCard(instructions: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top, // Align icon to top
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
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
private fun GameModeSelectionSection(
    currentMode: GameMode,
    onModeSelect: (GameMode) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.home_select_game_mode),
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameMode.entries.forEach { mode ->
                val selected = currentMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { onModeSelect(mode) },
                    label = {
                        Text(
                            text = stringResource(if (mode == GameMode.NORMAL) R.string.game_mode_normal else R.string.game_mode_reverse),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = if (selected) {
                        { Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null) }
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
                    label = { Text("$count", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = if (selected) {
                        { Icon(imageVector = Icons.Outlined.Star, contentDescription = null) }
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

/* ============
   GAME SCREEN
   ============ */
@Composable
fun GameScreen(vm: GameViewModel) {
    val q by vm.currentQuestion.collectAsState()
    val index by vm.currentIndex.collectAsState()
    val total by vm.totalQuestions.collectAsState()
    val isAnswered by vm.answered.collectAsState()
    val gameMode by vm.gameMode.collectAsState()

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
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(q.color),
                    contentAlignment = Alignment.Center
                ) {}
            }
        } else { // REVERSE mode
            Text(
                text = q.prompt, // e.g., "Which of these is Red?"
                style = MaterialTheme.typography.headlineSmall, // Larger text for question
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp) // Give it some space
            )
        }

        // Options
        if (gameMode == GameMode.NORMAL) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                q.options.forEach { opt ->
                    Button(
                        onClick = { vm.submitAnswer(opt as String) }, // opt is String
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isAnswered,
                        colors = ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text((opt as String), modifier = Modifier.padding(vertical = 6.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else { // REVERSE mode - Color options
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                q.options.forEach { opt ->
                    val colorOption = opt as Color
                    Card(
                        modifier = Modifier
                            .size(72.dp) // Square color patches
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isAnswered) { vm.submitAnswer(colorOption) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(if (isAnswered && vm.selectedAnswer.value == colorOption) 6.dp else 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().background(colorOption))
                    }
                }
            }
        }

        val correct by vm.correctCount.collectAsState()
        Spacer(modifier = Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.game_correct_count, correct), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { vm.skipQuestion() }, enabled = !isAnswered) {
                Text(stringResource(R.string.game_skip_button), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/* ==============
   RESULT SCREEN
   ============== */
@Composable
fun ResultScreen(vm: GameViewModel, onRestart: () -> Unit) {
    val score = vm.computeFinalScore()
    val summary = vm.summaryText()
    val incorrectAnswers by vm.incorrectAnswers.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.result_score, score), style = MaterialTheme.typography.titleMedium)

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

        Button(onClick = onRestart) {
            Text(stringResource(R.string.result_restart_button), style = MaterialTheme.typography.bodyMedium)
        }

        if (incorrectAnswers.isNotEmpty()) {
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
    }
}

@Composable
private fun IncorrectAnswerReviewCard(incorrectAnswer: IncorrectAnswer) {
    ElevatedCard(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)) // Use a distinct color
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (incorrectAnswer.gameMode == GameMode.NORMAL) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(56.dp).background(incorrectAnswer.question.color, RoundedCornerShape(8.dp))
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
            } else { // REVERSE Mode
                Text(
                    text = incorrectAnswer.question.prompt, // "Which of these is Red?"
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.result_your_answer_color, ""), // Removed %s as color patch is enough
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(incorrectAnswer.selectedAnswer as Color, RoundedCornerShape(8.dp))
                        )
                    }
                     Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                         Text(
                            text = stringResource(R.string.result_correct_answer_color, ""), // Removed %s
                            style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(incorrectAnswer.question.color, RoundedCornerShape(8.dp)) // Correct color
                        )
                    }
                }
                 if (incorrectAnswer.selectedAnswer == Color.Transparent) { // If skipped in reverse mode
                    Text(
                        text = stringResource(R.string.answer_skipped_color),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
