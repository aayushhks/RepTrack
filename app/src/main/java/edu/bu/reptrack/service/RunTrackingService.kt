package edu.bu.reptrack.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import edu.bu.reptrack.MainActivity
import edu.bu.reptrack.R
import edu.bu.reptrack.RepTrackApplication
import edu.bu.reptrack.data.run.ActiveRun
import edu.bu.reptrack.domain.run.RunFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class AndroidRunServiceController(private val context: Context) : RunServiceController {
    override fun start() {
        ContextCompat.startForegroundService(context, Intent(context, RunTrackingService::class.java))
    }
}

/**
 * Foreground service of type location. While a run is recorded it keeps the app alive with the
 * screen off and shows an ongoing notification with the distance and a live timer, so the user
 * always knows that tracking is on. The recording itself happens in RunRecorder; this service
 * only watches it and stops itself when the run ends.
 */
class RunTrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watchJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val recorder = (application as RepTrackApplication).container.runRecorder
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        // Android requires startForeground soon after startForegroundService, even if the run
        // already ended, so the notification is always shown first. The watcher below then
        // stops the service as soon as there is no run.
        val shown = recorder.active.value ?: ActiveRun(startedAt = System.currentTimeMillis())
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(shown), type)

        watchJob?.cancel()
        watchJob = scope.launch {
            // Redraw only when the shown distance changes (every 10 m), not on every fix.
            recorder.active
                .distinctUntilChangedBy { it?.progress?.distanceM?.div(10)?.toInt() }
                .collect { active ->
                    if (active == null) {
                        stopSelf()
                    } else if (NotificationManagerCompat.from(this@RunTrackingService).areNotificationsEnabled()) {
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(active))
                    }
                }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(run: ActiveRun): Notification {
        // Opens the app where the user left it, so the run screen comes back.
        val openApp = packageManager.getLaunchIntentForPackage(packageName) ?: Intent(this, MainActivity::class.java)
        val tap = PendingIntent.getActivity(this, 0, openApp, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.run_notification_title))
            .setContentText(getString(R.string.run_notification_text, RunFormat.km(run.progress.distanceM)))
            .setWhen(run.startedAt)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tap)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "run_tracking"
        private const val NOTIFICATION_ID = 1

        /** Called once at app start. Low importance: the notification is shown without a sound. */
        fun createChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.run_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.run_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
