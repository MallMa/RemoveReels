package io.github.mallma.removereels.guard

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.mallma.removereels.container
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

/**
 * Every 15 minutes, checks that the accessibility service is running and shows or removes the "Blocking is off"
 * notification. This catches what the service cannot report itself, such as a crash after which Android does not start
 * it again.
 */
internal class BlockingCheckWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val running = applicationContext.container.guardStatus.running
        // The worker may have started the app's process, and the service connects to it a moment later.
        val blockingOn = withTimeoutOrNull(CONNECT_GRACE) { running.first { it } } ?: false
        BlockingOffNotification(applicationContext).update(blockingOn)
        return Result.success()
    }

    companion object {
        private const val NAME = "blocking-check"

        /** The shortest interval WorkManager allows. */
        private const val MINIMUM_INTERVAL_MINUTES = 15L

        private val CONNECT_GRACE = 10.seconds

        /**
         * Schedules the check. The service calls this when it connects and the app once onboarding is done; an existing
         * schedule is kept, and WorkManager keeps it across restarts.
         */
        fun schedule(context: Context) {
            val request =
                PeriodicWorkRequestBuilder<BlockingCheckWorker>(
                    MINIMUM_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
