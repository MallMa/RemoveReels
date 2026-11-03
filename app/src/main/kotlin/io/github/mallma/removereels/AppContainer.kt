package io.github.mallma.removereels

import android.content.Context
import android.provider.Settings
import androidx.datastore.dataStoreFile
import io.github.mallma.removereels.detection.BundledRules
import io.github.mallma.removereels.detection.SurfaceDetector
import io.github.mallma.removereels.storage.StateStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.TimeSource

/** Creates the app's long-lived objects once and hands them to the service and the UI (ADR 0005). */
internal class AppContainer(
    context: Context,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    val clock: Clock = DeviceClock
    val timeSource: TimeSource = TimeSource.Monotonic
    val stateStore = StateStore.create(context.dataStoreFile("state.json"), scope)
    val detector = SurfaceDetector(BundledRules.load())
    val automaticTime =
        AutomaticTime { Settings.Global.getInt(context.contentResolver, Settings.Global.AUTO_TIME, 0) == 1 }
}

/** Whether Android sets the date and time from the network, which allowances need (ADR 0003). */
internal fun interface AutomaticTime {
    fun isOn(): Boolean
}

/** The system clock in the phone's current time zone; `Clock.systemDefaultZone()` would keep the zone it started in. */
private object DeviceClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()

    override fun withZone(zone: ZoneId): Clock = system(zone)

    override fun instant(): Instant = Instant.now()
}
