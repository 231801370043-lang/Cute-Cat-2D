package com.example.cutecat2d

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.cutecat2d.audio.SoundManager
import com.example.cutecat2d.data.GamePreferences
import com.example.cutecat2d.ui.*
import com.example.cutecat2d.ui.theme.CuteCat2DTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferences: GamePreferences
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        preferences = GamePreferences(this)
        soundManager = SoundManager.initialize(this)
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isMusicEnabled = preferences.isMusicEnabled

        if (preferences.isMusicEnabled) {
            soundManager.startBgm()
        }

        setContent {
            CuteCat2DTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(AppScreen.INTRO) }
                    var selectedLevelNumber by remember { mutableIntStateOf(1) }

                    // Hardware back button behavior
                    BackHandler(enabled = currentScreen != AppScreen.INTRO) {
                        when (currentScreen) {
                            AppScreen.GAMEPLAY -> currentScreen = AppScreen.LEVEL_SELECT
                            AppScreen.LEVEL_SELECT, AppScreen.INSTRUCTIONS -> currentScreen = AppScreen.INTRO
                            else -> {}
                        }
                    }

                    Crossfade(
                        targetState = currentScreen,
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.INTRO -> {
                                IntroScreen(
                                    preferences = preferences,
                                    onStartAdventure = {
                                        // Start with the latest unlocked level
                                        selectedLevelNumber = preferences.unlockedLevel.coerceIn(1, 20)
                                        currentScreen = AppScreen.GAMEPLAY
                                    },
                                    onHowToPlay = {
                                        currentScreen = AppScreen.INSTRUCTIONS
                                    },
                                    onLevelSelect = {
                                        currentScreen = AppScreen.LEVEL_SELECT
                                    }
                                )
                            }
                            AppScreen.INSTRUCTIONS -> {
                                InstructionsScreen(
                                    onBack = {
                                        currentScreen = AppScreen.INTRO
                                    },
                                    onPlay = {
                                        selectedLevelNumber = preferences.unlockedLevel.coerceIn(1, 20)
                                        currentScreen = AppScreen.GAMEPLAY
                                    }
                                )
                            }
                            AppScreen.LEVEL_SELECT -> {
                                LevelSelectScreen(
                                    preferences = preferences,
                                    onBack = {
                                        currentScreen = AppScreen.INTRO
                                    },
                                    onSelectLevel = { levelNum ->
                                        selectedLevelNumber = levelNum
                                        currentScreen = AppScreen.GAMEPLAY
                                    }
                                )
                            }
                            AppScreen.GAMEPLAY -> {
                                GamePlayScreen(
                                    levelNumber = selectedLevelNumber,
                                    preferences = preferences,
                                    onBackToLevelSelect = {
                                        currentScreen = AppScreen.LEVEL_SELECT
                                    },
                                    onBackToHome = {
                                        currentScreen = AppScreen.INTRO
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        if (::soundManager.isInitialized && ::preferences.isInitialized && preferences.isMusicEnabled) {
            soundManager.startBgm()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::soundManager.isInitialized) {
            soundManager.stopBgm()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::soundManager.isInitialized) {
            soundManager.stopBgm()
        }
    }
}
