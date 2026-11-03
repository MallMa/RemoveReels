package io.github.mallma.removereels.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.mallma.removereels.R
import io.github.mallma.removereels.budget.TargetApp
import io.github.mallma.removereels.packageInfoOf

/** The app's name, such as "Instagram". */
internal val TargetApp.label: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.app_instagram
            TargetApp.YOUTUBE -> R.string.app_youtube
        }

/** What the app's short videos are called: "Reels" or "Shorts". */
internal val TargetApp.surfaceLabel: Int
    get() =
        when (this) {
            TargetApp.INSTAGRAM -> R.string.surface_instagram
            TargetApp.YOUTUBE -> R.string.surface_youtube
        }

/**
 * The installed app's own icon, read at runtime so that no logo ships with RemoveReels. Shows an empty space of the
 * same size if the app is not installed.
 */
@Composable
internal fun AppIcon(
    app: TargetApp,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val pixels = with(LocalDensity.current) { size.roundToPx() }
    val icon = remember(app, pixels) { context.loadIcon(app)?.toBitmap(pixels, pixels)?.asImageBitmap() }
    if (icon == null) {
        Box(modifier.size(size))
    } else {
        Image(bitmap = icon, contentDescription = null, modifier = modifier.size(size))
    }
}

private fun Context.loadIcon(app: TargetApp): Drawable? =
    packageManager.packageInfoOf(app)?.applicationInfo?.loadIcon(packageManager)
