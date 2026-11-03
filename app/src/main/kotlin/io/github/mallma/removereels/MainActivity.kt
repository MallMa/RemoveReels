package io.github.mallma.removereels

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import io.github.mallma.removereels.ui.RemoveReelsApp
import io.github.mallma.removereels.ui.theme.RemoveReelsTheme

class MainActivity : ComponentActivity() {
    /** Set by the "Blocking is off" notification, which opens the Turn on screen. */
    private val openTurnOn = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity has handled its intent already.
        if (savedInstanceState == null) openTurnOn.value = intent.opensTurnOn()
        setContent {
            RemoveReelsTheme {
                RemoveReelsApp(openTurnOn = openTurnOn.value, onTurnOnOpened = { openTurnOn.value = false })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openTurnOn.value = intent.opensTurnOn()
    }

    /** From the notification itself, not from Recents, which replays the intent the task was started with. */
    private fun Intent.opensTurnOn(): Boolean =
        getBooleanExtra(EXTRA_OPEN_TURN_ON, false) && flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0

    internal companion object {
        const val EXTRA_OPEN_TURN_ON = "io.github.mallma.removereels.OPEN_TURN_ON"
    }
}
