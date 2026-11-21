package com.huy.unlockvocab.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.huy.unlockvocab.data.prefs.SettingsStore

class ServiceRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = SettingsStore(context).load()
        if (!settings.serviceEnabled || !Settings.canDrawOverlays(context)) {
            ServiceWatchdog.cancelAll(context)
            return
        }
        if (!UnlockService.isRunning) {
            UnlockService.startSafe(context, fromBackground = true, reason = "restart_alarm")
        }
        ServiceWatchdog.schedulePeriodic(context)
    }
}
