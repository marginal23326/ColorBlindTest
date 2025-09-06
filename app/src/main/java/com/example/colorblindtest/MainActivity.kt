package com.example.colorblindtest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.colorblindtest.model.Screen
import com.example.colorblindtest.ui.screen.GameScreen
import com.example.colorblindtest.ui.screen.HomeScreen
import com.example.colorblindtest.ui.screen.ResultScreen
import com.example.colorblindtest.ui.theme.ColorBlindTestTheme

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
