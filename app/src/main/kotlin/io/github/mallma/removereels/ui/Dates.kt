package io.github.mallma.removereels.ui

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A day with its weekday, such as "Tue 3 Nov", in the order [locale] uses. */
internal fun LocalDate.formatShort(locale: Locale): String =
    format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale))

/** [formatShort] in the phone's language. */
@Composable
internal fun shortDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    return remember(date, locale) { date.formatShort(locale) }
}
