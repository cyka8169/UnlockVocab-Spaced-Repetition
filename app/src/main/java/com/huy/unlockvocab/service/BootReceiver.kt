package com.huy.unlockvocab.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.huy.unlockvocab.data.prefs.SettingsStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val settings = SettingsStore(context).load()
            if (settings.serviceEnabled) {
                UnlockService.start(context)
            }
        }
    }
}
