package com.example.colorblindtest.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colorblindtest.GameViewModel
import com.example.colorblindtest.R
import com.example.colorblindtest.model.Difficulty
import com.example.colorblindtest.model.GameMode
import java.util.Locale

@Composable
fun HomeScreen(vm: GameViewModel, onStart: () -> Unit) {
    val currentTotalQuestions by vm.totalQuestions.collectAsState()
    val highScore by vm.highScore.collectAsState()
    val highScoreAverageTime by vm.highScoreAverageTime.collectAsState() // Collect average time
    val questionOptions = listOf(5, 10, 15, 20)
    val currentGameMode by vm.gameMode.collectAsState()
    val currentDifficulty by vm.difficulty.collectAsState()

    Scaffold(
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
                InfoCard(
                    icon = Icons.Outlined.Star,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_high_score, highScore),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (highScoreAverageTime >= 0) String.format(
                                Locale.US,
                                stringResource(R.string.summary_average_time),
                                highScoreAverageTime
                            )
                            else stringResource(R.string.summary_average_time_na),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    TextButton(onClick = { vm.clearHighScore() }) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = stringResource(R.string.home_clear_high_score_button))
                    }
                }
            }

            InfoCard(
                icon = Icons.Outlined.Lightbulb,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = stringResource(R.string.home_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            GameModeSelectionSection(
                currentMode = currentGameMode,
                onModeSelect = { vm.setGameMode(it) }
            )

            AnimatedVisibility(visible = currentGameMode == GameMode.SHADE) {
                DifficultySelectionSection(
                    currentDifficulty = currentDifficulty,
                    onDifficultySelect = { vm.setDifficulty(it) }
                )
            }

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
private fun InfoCard(
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = verticalAlignment,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = if (verticalAlignment == Alignment.Top) Modifier.padding(top = 2.dp) else Modifier
            )
            content()
        }
    }
}

@Composable
private fun GameModeSelectionSection(
    currentMode: GameMode,
    onModeSelect: (GameMode) -> Unit
) {
    SelectionSection(title = stringResource(R.string.home_select_game_mode)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GameMode.entries.forEach { mode ->
                OptionChip(
                    option = mode,
                    currentSelection = currentMode,
                    onSelect = onModeSelect,
                    label = {
                        Text(
                            text = stringResource(
                                when (mode) {
                                    GameMode.NORMAL -> R.string.game_mode_normal
                                    GameMode.REVERSE -> R.string.game_mode_reverse
                                    GameMode.SHADE -> R.string.game_mode_shade
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    selectedIcon = Icons.Filled.CheckCircle
                )
            }
        }
    }
}

@Composable
private fun DifficultySelectionSection(
    currentDifficulty: Difficulty,
    onDifficultySelect: (Difficulty) -> Unit
) {
    SelectionSection(title = stringResource(R.string.home_select_difficulty)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.entries.forEach { difficulty ->
                OptionChip(
                    option = difficulty,
                    currentSelection = currentDifficulty,
                    onSelect = onDifficultySelect,
                    label = {
                        Text(
                            text = stringResource(
                                when (difficulty) {
                                    Difficulty.MEDIUM -> R.string.difficulty_medium
                                    Difficulty.HARD -> R.string.difficulty_hard
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    selectedIcon = Icons.Filled.CheckCircle
                )
            }
        }
    }
}


@Composable
private fun QuestionSelectionSection(
    current: Int,
    options: List<Int>,
    onSelect: (Int) -> Unit
) {
    SelectionSection(title = stringResource(R.string.home_number_of_questions, current)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(options) { count ->
                OptionChip(
                    option = count,
                    currentSelection = current,
                    onSelect = onSelect,
                    label = { Text("$count", style = MaterialTheme.typography.bodyMedium) },
                    selectedIcon = Icons.Outlined.Star
                )
            }
        }
    }
}

@Composable
private fun <T> OptionChip(
    option: T,
    currentSelection: T,
    onSelect: (T) -> Unit,
    label: @Composable () -> Unit,
    selectedIcon: ImageVector
) {
    val selected = currentSelection == option
    FilterChip(
        selected = selected,
        onClick = { onSelect(option) },
        label = label,
        leadingIcon = if (selected) {
            { Icon(imageVector = selectedIcon, contentDescription = null) }
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

@Composable
private fun SelectionSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}
