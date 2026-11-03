package io.github.mallma.removereels.guard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Holds transient audio focus, which makes Instagram and YouTube pause their video until it is released. */
internal class AudioFocus(
    context: Context,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val request =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .build()
    private var held = false

    fun hold() {
        if (!held) held = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun release() {
        if (held) audioManager.abandonAudioFocusRequest(request)
        held = false
    }
}
