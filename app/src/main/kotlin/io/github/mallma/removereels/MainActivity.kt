package io.github.mallma.removereels

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.mallma.removereels.ui.RemoveReelsApp
import io.github.mallma.removereels.ui.theme.RemoveReelsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            RemoveReelsTheme {
                RemoveReelsApp()
            }
        }
    }
}
