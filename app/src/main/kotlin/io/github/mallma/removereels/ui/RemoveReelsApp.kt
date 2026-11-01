package io.github.mallma.removereels.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mallma.removereels.R

/** Root of the app's user interface. */
@Composable
fun RemoveReelsApp(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier) { contentPadding ->
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(contentPadding).padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}
