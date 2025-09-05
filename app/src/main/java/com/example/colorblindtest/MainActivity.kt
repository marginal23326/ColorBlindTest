package com.example.colorblindtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun HomeScreen(vm: GameViewModel, onStart: () -> Unit) {
    val currentTotalQuestions by vm.totalQuestions.collectAsState()
    val questionOptions = listOf(5, 10, 15, 20) // Options for number of questions

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Color Blindness Quick Test", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "You will be shown a color patch and several choices for its name. " +
                    "Try to pick the correct name as fast as you can."
        )
        Spacer(modifier = Modifier.height(30.dp))

        Text("Number of Questions: $currentTotalQuestions", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            questionOptions.forEach { count ->
                Button(
                    onClick = { vm.setTotalQuestions(count) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (currentTotalQuestions == count) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (currentTotalQuestions == count) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("$count")
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("Start Test with $currentTotalQuestions Questions", fontSize = 16.sp)
        }
    }
}

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

    Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question ${index + 1} / $total")
            Text("Time: ${elapsedDisplay}s")
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(q.color, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {}
        Spacer(Modifier.height(12.dp))
        Column {
            q.options.forEach { opt ->
                Button(
                    onClick = { vm.submitAnswer(opt) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    enabled = !isAnswered
                ) {
                    Text(opt, modifier = Modifier.padding(8.dp))
                }
            }
        }
        val correct by vm.correctCount.collectAsState()

        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Correct: $correct")
            Button(onClick = { vm.skipQuestion() }, enabled = !isAnswered) {
                Text("Skip")
            }
        }
    }
}

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
        // Removed Arrangement.Center to allow content to flow from the top
    ) {
        Text("Result", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Score: ${"%.0f".format(score)} / 100", fontSize = 22.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(summary)
        Spacer(modifier = Modifier.height(18.dp))
        Button(onClick = onRestart) { Text("Restart test") }
        Spacer(modifier = Modifier.height(20.dp))

        if (incorrectAnswers.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            Text("Review Incorrect Answers:", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(incorrectAnswers) { incorrectAnswer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(incorrectAnswer.question.color, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text("Your answer: ${incorrectAnswer.selectedAnswer}", fontSize = 16.sp)
                            Text("Correct answer: ${incorrectAnswer.question.correctName}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

