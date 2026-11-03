package io.github.mallma.removereels.guard

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class BlockingOffNotificationTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notification = BlockingOffNotification(context)

    @Test
    fun `tells the user when blocking is off`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notification.update(blockingOn = false)

        val posted = shadowOf(manager).allNotifications.single()
        assertThat(shadowOf(posted).contentTitle).isEqualTo("Blocking is off")
        assertThat(shadowOf(posted).contentText).isEqualTo("Tap to turn it back on.")
    }

    @Test
    fun `goes away once blocking is on again`() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notification.update(blockingOn = false)

        notification.update(blockingOn = true)

        assertThat(shadowOf(manager).allNotifications).isEmpty()
    }

    @Test
    fun `stays silent without the notification permission`() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notification.update(blockingOn = false)

        assertThat(shadowOf(manager).allNotifications).isEmpty()
    }
}
