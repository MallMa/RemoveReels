package io.github.mallma.removereels

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import io.github.mallma.removereels.guard.BlockingCheckWorker
import io.github.mallma.removereels.ui.RemoveReelsApp
import io.github.mallma.removereels.ui.theme.RemoveReelsTheme

class MainActivity : ComponentActivity() {
    /** Set by the "Blocking is off" notification, which opens the Turn on screen. */
    private val openTurnOn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        BlockingCheckWorker.schedule(this)
        openTurnOn.value = intent.getBooleanExtra(EXTRA_OPEN_TURN_ON, false)
        setContent {
            RemoveReelsTheme {
                RemoveReelsApp(openTurnOn = openTurnOn.value, onTurnOnOpened = { openTurnOn.value = false })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openTurnOn.value = intent.getBooleanExtra(EXTRA_OPEN_TURN_ON, false)
    }

    internal companion object {
        const val EXTRA_OPEN_TURN_ON = "io.github.mallma.removereels.OPEN_TURN_ON"
    }
}
