package com.huy.unlockvocab.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.huy.unlockvocab.data.prefs.SettingsStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action in SUPPORTED_ACTIONS) {
            val settings = SettingsStore(context).load()
            if (settings.serviceEnabled) {
                UnlockService.startSafe(context, fromBackground = true, reason = action)
                ServiceWatchdog.schedulePeriodic(context)
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_UNLOCKED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )
    }
}
