package com.huy.unlockvocab.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * Dual-layer watchdog (JobScheduler every 15 minutes + AlarmManager on task/service removal)
 * to recover UnlockService when OEM task cleaners kill background processes.
 */
object ServiceWatchdog {
    private const val TAG = "UnlockVocab"
    const val JOB_ID_WATCHDOG = 2041
    const val REQ_CODE_RESTART_ALARM = 2042
    const val ACTION_WATCHDOG_RESTART = "com.huy.unlockvocab.ACTION_WATCHDOG_RESTART"
    private const val PERIODIC_INTERVAL_MS = 15 * 60_000L

    fun schedulePeriodic(context: Context) {
        try {
            val appContext = context.applicationContext
            val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            if (scheduler.getPendingJob(JOB_ID_WATCHDOG) != null) return

            val job = JobInfo.Builder(
                JOB_ID_WATCHDOG,
                ComponentName(appContext, ServiceWatchdogJobService::class.java)
            )
                .setPeriodic(PERIODIC_INTERVAL_MS)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .build()

            val result = scheduler.schedule(job)
            Log.d(TAG, "ServiceWatchdog periodic scheduled result=$result")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule periodic watchdog: ${e.message}")
        }
    }

    fun scheduleImmediateRestart(context: Context, delayMs: Long = 2_500L) {
        try {
            val appContext = context.applicationContext
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val pi = restartPendingIntent(appContext)
            val triggerAt = SystemClock.elapsedRealtime() + delayMs.coerceAtLeast(1_000L)
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
            Log.d(TAG, "ServiceWatchdog restart alarm scheduled in ${delayMs}ms")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule restart alarm: ${e.message}")
        }
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        try {
            val scheduler = appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler
            scheduler?.cancel(JOB_ID_WATCHDOG)
        } catch (_: Exception) {
        }
        try {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.cancel(restartPendingIntent(appContext))
        } catch (_: Exception) {
        }
    }

    private fun restartPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ServiceRestartReceiver::class.java).apply {
            action = ACTION_WATCHDOG_RESTART
        }
        return PendingIntent.getBroadcast(
            context,
            REQ_CODE_RESTART_ALARM,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
