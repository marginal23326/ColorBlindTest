package com.example.colorblindtest.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.colorblindtest.GameViewModel
import com.example.colorblindtest.R
import com.example.colorblindtest.model.GameMode
import java.util.Locale

@Composable
fun HomeScreen(vm: GameViewModel, onStart: () -> Unit) {
    val currentTotalQuestions by vm.totalQuestions.collectAsState()
    val highScore by vm.highScore.collectAsState()
    val highScoreAverageTime by vm.highScoreAverageTime.collectAsState() // Collect average time
    val questionOptions = listOf(5, 10, 15, 20)
    val currentGameMode by vm.gameMode.collectAsState()

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
