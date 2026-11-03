package io.github.mallma.removereels.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A day with its weekday, such as "Tue 3 Nov", in the order the phone's language uses. */
@Composable
internal fun shortDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter =
        remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale) }
    return date.format(formatter)
}
