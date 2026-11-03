package io.github.mallma.removereels.guard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import io.github.mallma.removereels.MainActivity
import io.github.mallma.removereels.R

/** The app's only notification: "Blocking is off. Tap to turn it back on." */
internal class BlockingOffNotification(
    private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    private val mayNotify: Boolean
        get() = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Shows the notification while blocking is off and removes it once blocking is on. */
    fun update(blockingOn: Boolean) {
        when {
            blockingOn -> {
                manager.cancel(ID)
            }

            mayNotify -> {
                createChannel()
                manager.notify(ID, build())
            }
        }
    }

    private fun createChannel() {
        val channel =
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notification_channel_description) }
        manager.createNotificationChannel(channel)
    }

    private fun build(): Notification {
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
        return Notification
            .Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_blocking_off))
            .setContentText(context.getString(R.string.notification_tap_to_turn_on))
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL = "blocking"
        const val ID = 1
    }
}
