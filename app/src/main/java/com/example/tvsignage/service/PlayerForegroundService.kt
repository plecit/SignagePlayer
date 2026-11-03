package com.example.tvsignage.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.tvsignage.R
import com.example.tvsignage.SignageApplication
import com.example.tvsignage.core.HardwareKeyManager
import com.example.tvsignage.core.SecurePreferences
import com.example.tvsignage.core.SystemLogger
import com.example.tvsignage.files.CachePurgeManager
import com.example.tvsignage.files.DownloadManager
import com.example.tvsignage.files.LocalStorageManager
import com.example.tvsignage.report.StatusNotifier
import com.example.tvsignage.schedule.ScheduleManager
import com.example.tvsignage.stats.ProofOfPlayEngine
import com.example.tvsignage.ui.PlayerActivity
import com.example.tvsignage.xmds.XmdsClient
import com.example.tvsignage.xmds.models.ActivationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var securePrefs: SecurePreferences
    private lateinit var xmdsClient: XmdsClient
    private lateinit var storageManager: LocalStorageManager
    private lateinit var downloadManager: DownloadManager
    private lateinit var purgeManager: CachePurgeManager
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var popEngine: ProofOfPlayEngine
    private lateinit var statusNotifier: StatusNotifier

    private var syncJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as SignageApplication
        securePrefs = app.securePreferences
        storageManager = LocalStorageManager(this)
        xmdsClient = XmdsClient(securePrefs)
        downloadManager = DownloadManager(xmdsClient, storageManager)
        purgeManager = CachePurgeManager(storageManager)
        scheduleManager = ScheduleManager(storageManager)
        popEngine = ProofOfPlayEngine(app.statDatabase.statDao(), xmdsClient, securePrefs)
        statusNotifier = StatusNotifier(this, xmdsClient)

        startForeground(NOTIFICATION_ID, createNotification("Starting signage service..."))
        startSyncLoop()
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            val hardwareKey = HardwareKeyManager.getHardwareKey(this@PlayerForegroundService)

            while (isActive) {
                if (!securePrefs.isConfigured()) {
                    SystemLogger.i(TAG, "Player not configured yet, waiting...")
                    delay(5000)
                    continue
                }

                try {
                    SystemLogger.i(TAG, "Starting XMDS sync cycle...")

                    // 1. RegisterDisplay
                    val regResult = xmdsClient.registerDisplay(hardwareKey)
                    securePrefs.isAuthorised = (regResult.status == ActivationStatus.READY)

                    // Apply updated settings from CMS
                    regResult.settings["collectInterval"]?.toIntOrNull()?.let {
                        securePrefs.collectIntervalSeconds = it
                    }
                    regResult.settings["statsEnabled"]?.let {
                        securePrefs.statsEnabled = (it == "1" || it.equals("true", ignoreCase = true))
                    }

                    broadcastSyncStatus(regResult.status)

                    if (regResult.status == ActivationStatus.READY) {
                        // 2. RequiredFiles
                        val (requiredFiles, purgeList) = xmdsClient.getRequiredFiles(hardwareKey)
                        purgeManager.purgeExplicit(purgeList)

                        // 3. Schedule
                        val scheduleData = xmdsClient.getSchedule(hardwareKey)
                        scheduleManager.updateSchedule(scheduleData)

                        // 4. Download missing files
                        val inventory = downloadManager.syncFiles(hardwareKey, requiredFiles)

                        // 5. Submit inventory
                        xmdsClient.submitMediaInventory(hardwareKey, inventory)

                        // 6. Submit proof of play
                        popEngine.flushPendingStats(hardwareKey)

                        // 7. Submit logs
                        val logs = SystemLogger.drainLogs(50)
                        xmdsClient.submitLog(hardwareKey, logs)

                        // 8. Notify status
                        val currentActiveLayout = scheduleManager.getActiveLayout()
                        statusNotifier.sendStatus(hardwareKey, currentActiveLayout)

                        // 9. Notify UI to refresh layout
                        val intent = Intent(ACTION_SCHEDULE_UPDATED)
                        sendBroadcast(intent)
                    }

                } catch (e: Exception) {
                    SystemLogger.w(TAG, "XMDS sync cycle failed: ${e.message}")
                }

                val sleepSeconds = securePrefs.collectIntervalSeconds.coerceAtLeast(10)
                delay(sleepSeconds * 1000L)
            }
        }
    }

    private fun broadcastSyncStatus(status: ActivationStatus) {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra(EXTRA_ACTIVATION_STATUS, status.name)
        }
        sendBroadcast(intent)
    }

    private fun createNotification(contentText: String): Notification {
        val channelId = "signage_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "TV Signage Playback Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TV Signage Player")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlayerService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_STATUS_CHANGED = "com.example.tvsignage.ACTION_STATUS_CHANGED"
        const val ACTION_SCHEDULE_UPDATED = "com.example.tvsignage.ACTION_SCHEDULE_UPDATED"
        const val EXTRA_ACTIVATION_STATUS = "extra_activation_status"
    }
}
