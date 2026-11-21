package com.huy.unlockvocab.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.provider.Settings
import com.huy.unlockvocab.data.prefs.SettingsStore

class ServiceWatchdogJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        val settings = SettingsStore(this).load()
        if (settings.serviceEnabled && Settings.canDrawOverlays(this)) {
            if (!UnlockService.isRunning) {
                UnlockService.startSafe(this, fromBackground = true, reason = "watchdog_job")
            }
        } else if (!settings.serviceEnabled) {
            ServiceWatchdog.cancelAll(this)
        }
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false
}
