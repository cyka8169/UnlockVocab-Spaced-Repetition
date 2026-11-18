package com.huy.unlockvocab.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.huy.unlockvocab.MainActivity
import com.huy.unlockvocab.R
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.prefs.SettingsStore
import com.huy.unlockvocab.ui.overlay.QuizOverlayManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground Service keeping an active BroadcastReceiver for ACTION_USER_PRESENT
 * (screen unlock) and ACTION_SCREEN_ON (when no lockscreen PIN is set).
 */
class UnlockService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var overlayManager: QuizOverlayManager
    private lateinit var repository: VocabRepository
    private val mainHandler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false
    private var triggeredInCurrentSession = false
    private var keyguardWatchRunnable: Runnable? = null

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == Intent.ACTION_SCREEN_OFF) {
                triggeredInCurrentSession = false
                stopKeyguardWatcher()
                overlayManager.dismiss(recordDismiss = false)
                return
            }

            if (action == Intent.ACTION_USER_PRESENT) {
                if (!triggeredInCurrentSession) {
                    triggeredInCurrentSession = true
                    stopKeyguardWatcher()
                    handlePotentialUnlock("ACTION_USER_PRESENT")
                }
            } else if (action == Intent.ACTION_SCREEN_ON) {
                val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (!km.isKeyguardLocked) {
                    if (!triggeredInCurrentSession) {
                        triggeredInCurrentSession = true
                        stopKeyguardWatcher()
                        handlePotentialUnlock("ACTION_SCREEN_ON_UNLOCKED")
                    }
                } else {
                    startKeyguardWatcher()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repository = VocabRepository.getInstance(this)
        overlayManager = QuizOverlayManager(this)
        createNotificationChannel()
        startForegroundCompat()
        registerUnlockReceiver()
        serviceScope.launch(Dispatchers.IO) {
            repository.ensureSeeded()
        }
        Log.d(TAG, "UnlockService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TEST_QUIZ -> {
                serviceScope.launch {
                    val authentic = withContext(Dispatchers.IO) {
                        repository.verifyOwnershipIntegrity(forceRefresh = false).isAuthentic
                    }
                    if (authentic) {
                        overlayManager.showNextQuiz(isManualTest = true)
                    } else {
                        Log.w(TAG, "Blocked overlay quiz: failed ownership integrity check")
                    }
                }
            }
            ACTION_SNOOZE_30M -> {
                repository.settingsStore.pauseForMinutes(30)
                overlayManager.dismiss(recordDismiss = false)
                refreshNotification()
            }
            ACTION_RESUME_NOW -> {
                repository.settingsStore.clearSnooze()
                refreshNotification()
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopKeyguardWatcher()
        if (receiverRegistered) {
            try {
                unregisterReceiver(unlockReceiver)
            } catch (_: Exception) {
            }
            receiverRegistered = false
        }
        overlayManager.destroy()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handlePotentialUnlock(source: String) {
        serviceScope.launch {
            val shouldShow = withContext(Dispatchers.IO) {
                val integrity = repository.verifyOwnershipIntegrity(forceRefresh = false)
                if (!integrity.isAuthentic) {
                    Log.w(TAG, "Blocked unlock overlay: ${integrity.reason}")
                    return@withContext false
                }
                val todayCount = repository.getTodayAnsweredCount()
                val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val inCall = audio.mode == AudioManager.MODE_IN_CALL ||
                    audio.mode == AudioManager.MODE_IN_COMMUNICATION ||
                    audio.mode == AudioManager.MODE_RINGTONE
                repository.settingsStore.shouldTriggerOnUnlock(todayCount, inCall)
            }
            Log.d(TAG, "Unlock event [$source] -> shouldShow=$shouldShow")
            refreshNotification()
            if (shouldShow) {
                overlayManager.showNextQuiz(isManualTest = false)
            }
        }
    }

    private fun startKeyguardWatcher() {
        stopKeyguardWatcher()
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        var attempts = 0
        val runnable = object : Runnable {
            override fun run() {
                if (triggeredInCurrentSession || !pm.isInteractive) {
                    stopKeyguardWatcher()
                    return
                }
                if (!km.isKeyguardLocked) {
                    triggeredInCurrentSession = true
                    stopKeyguardWatcher()
                    handlePotentialUnlock("KEYGUARD_UNLOCKED")
                    return
                }
                attempts++
                if (attempts < 25) {
                    mainHandler.postDelayed(this, 350L)
                } else {
                    keyguardWatchRunnable = null
                }
            }
        }
        keyguardWatchRunnable = runnable
        mainHandler.postDelayed(runnable, 350L)
    }

    private fun stopKeyguardWatcher() {
        keyguardWatchRunnable?.let { mainHandler.removeCallbacks(it) }
        keyguardWatchRunnable = null
    }

    private fun registerUnlockReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // RECEIVER_EXPORTED is required on Android 14+ so ACTION_USER_PRESENT from SystemUI (uid=10040) is delivered.
            registerReceiver(unlockReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(unlockReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Giữ chế độ bật trắc nghiệm từ vựng khi mở khóa máy"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val remainingSnooze = repository.settingsStore.getSnoozeRemainingMinutes()
        val isSnoozed = remainingSnooze > 0

        val toggleIntent = Intent(this, UnlockService::class.java).apply {
            action = if (isSnoozed) ACTION_RESUME_NOW else ACTION_SNOOZE_30M
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            1,
            toggleIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentText = if (isSnoozed) {
            "Đang tạm nghỉ (còn $remainingSnooze phút) — không bật câu hỏi."
        } else {
            getString(R.string.service_notification_text)
        }
        val actionLabel = if (isSnoozed) "Tiếp tục ngay" else "Tạm nghỉ 30 phút"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_unlock_vocab)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, actionLabel, togglePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun refreshNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startForegroundCompat() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    companion object {
        private const val TAG = "UnlockVocab"
        private const val CHANNEL_ID = "unlock_vocab_service_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_TEST_QUIZ = "com.huy.unlockvocab.ACTION_TEST_QUIZ"
        const val ACTION_SNOOZE_30M = "com.huy.unlockvocab.ACTION_SNOOZE_30M"
        const val ACTION_RESUME_NOW = "com.huy.unlockvocab.ACTION_RESUME_NOW"
        const val ACTION_STOP = "com.huy.unlockvocab.ACTION_STOP"

        fun start(context: Context) {
            val intent = Intent(context, UnlockService::class.java)
            context.startForegroundService(intent)
        }

        fun triggerTestQuiz(context: Context) {
            val intent = Intent(context, UnlockService::class.java).apply {
                action = ACTION_TEST_QUIZ
            }
            context.startForegroundService(intent)
        }

        fun snooze30Minutes(context: Context) {
            SettingsStore(context).pauseForMinutes(30)
            val intent = Intent(context, UnlockService::class.java).apply {
                action = ACTION_SNOOZE_30M
            }
            context.startForegroundService(intent)
        }

        fun resumeFromSnooze(context: Context) {
            SettingsStore(context).clearSnooze()
            val intent = Intent(context, UnlockService::class.java).apply {
                action = ACTION_RESUME_NOW
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UnlockService::class.java))
        }
    }
}
