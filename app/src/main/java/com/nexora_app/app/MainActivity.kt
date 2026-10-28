package com.nexora_app.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexora_app.app.navigation.EllieApp
import com.nexora_app.app.ui.theme.EllieTheme
import com.nexora_app.app.viewmodel.AudioManager
import com.nexora_app.app.viewmodel.ThemeManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Powers the speaker button on the flashcard deck (Words tab).
        // TEMP: Log.d added below (in AudioManager) so we can see in Logcat
        // exactly why pronounce() might be silently no-op-ing — remove once
        // confirmed working.
        AudioManager.init(applicationContext)

        setContent {
            val isDarkTheme by ThemeManager.isDarkTheme.collectAsStateWithLifecycle()

            EllieTheme(isDarkTheme = isDarkTheme) {
                EllieApp()
            }
        }
    }

    override fun onDestroy() {
        AudioManager.shutdown()
        super.onDestroy()
    }
}