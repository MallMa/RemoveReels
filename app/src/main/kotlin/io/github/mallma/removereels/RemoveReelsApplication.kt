package io.github.mallma.removereels

import android.app.Application
import android.content.Context

class RemoveReelsApplication : Application() {
    internal val container by lazy { AppContainer(this) }
}

internal val Context.container: AppContainer get() = (applicationContext as RemoveReelsApplication).container
