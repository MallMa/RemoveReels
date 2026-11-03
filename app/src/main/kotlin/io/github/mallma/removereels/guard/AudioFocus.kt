package io.github.mallma.removereels.guard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log

/** Holds transient audio focus, which makes Instagram and YouTube pause their video until it is released. */
internal class AudioFocus(
    context: Context,
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var held = false
    private val request =
        AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setOnAudioFocusChangeListener { change ->
                // Another app took the focus for good; holding it again needs a new request.
                if (change == AudioManager.AUDIOFOCUS_LOSS) held = false
            }.build()

    fun hold() {
        if (held) return
        held = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!held) Log.w(TAG, "Audio focus refused; leaving the screen stops the video instead")
    }

    fun release() {
        if (held) audioManager.abandonAudioFocusRequest(request)
        held = false
    }

    private companion object {
        const val TAG = "AudioFocus"
    }
}
