package io.github.mallma.removereels.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Amber for "almost out of time": dynamic colour schemes have no fixed warning hue. */
@Immutable
internal data class WarningColors(
    val container: Color,
    val content: Color,
)

private val LightWarningColors =
    WarningColors(container = Color(color = 0xFFFFE08A), content = Color(color = 0xFF3B2F00))
private val DarkWarningColors =
    WarningColors(container = Color(color = 0xFF574500), content = Color(color = 0xFFFFE08A))

@Composable
internal fun warningColors(): WarningColors = if (isSystemInDarkTheme()) DarkWarningColors else LightWarningColors
