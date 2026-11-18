package com.huy.unlockvocab.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.huy.unlockvocab.data.db.AppSettings
import com.huy.unlockvocab.data.db.TriggerMode
import com.huy.unlockvocab.data.db.WordSelectionPolicy
import java.util.Calendar

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("unlock_vocab_prefs", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true),
        triggerMode = enumValueOrDefault(prefs.getString(KEY_TRIGGER_MODE, null), TriggerMode.EVERY_UNLOCK),
        everyNUnlocks = prefs.getInt(KEY_EVERY_N, 2).coerceIn(1, 20),
        cooldownMinutes = prefs.getInt(KEY_COOLDOWN_MINUTES, 15).coerceIn(1, 180),
        selectionPolicy = enumValueOrDefault(prefs.getString(KEY_SELECTION_POLICY, null), WordSelectionPolicy.DUE_FIRST),
        filterPos = prefs.getString(KEY_FILTER_POS, "ALL") ?: "ALL",
        filterCefr = prefs.getString(KEY_FILTER_CEFR, "ALL") ?: "ALL",
        quietHoursEnabled = prefs.getBoolean(KEY_QUIET_ENABLED, false),
        quietStartHour = prefs.getInt(KEY_QUIET_START, 23).coerceIn(0, 23),
        quietEndHour = prefs.getInt(KEY_QUIET_END, 6).coerceIn(0, 23),
        dailyLimit = prefs.getInt(KEY_DAILY_LIMIT, 0).coerceAtLeast(0)
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_SERVICE_ENABLED, settings.serviceEnabled)
            .putString(KEY_TRIGGER_MODE, settings.triggerMode.name)
            .putInt(KEY_EVERY_N, settings.everyNUnlocks)
            .putInt(KEY_COOLDOWN_MINUTES, settings.cooldownMinutes)
            .putString(KEY_SELECTION_POLICY, settings.selectionPolicy.name)
            .putString(KEY_FILTER_POS, settings.filterPos)
            .putString(KEY_FILTER_CEFR, settings.filterCefr)
            .putBoolean(KEY_QUIET_ENABLED, settings.quietHoursEnabled)
            .putInt(KEY_QUIET_START, settings.quietStartHour)
            .putInt(KEY_QUIET_END, settings.quietEndHour)
            .putInt(KEY_DAILY_LIMIT, settings.dailyLimit)
            .apply()
    }

    fun getLastWordId(): Long = prefs.getLong(KEY_LAST_WORD_ID, -1L)

    fun setLastWordId(wordId: Long) {
        prefs.edit().putLong(KEY_LAST_WORD_ID, wordId).apply()
    }

    fun recordQuizShown(nowMs: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_SHOWN_AT, nowMs).apply()
    }

    /**
     * Evaluates whether the quiz overlay should appear on this unlock event,
     * respecting quiet hours, active calls, daily quota, and the chosen TriggerMode.
     */
    fun shouldTriggerOnUnlock(
        todayAnsweredCount: Int,
        isInCall: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val s = load()
        if (!s.serviceEnabled || isInCall) return false
        if (s.quietHoursEnabled && isWithinQuietHours(s.quietStartHour, s.quietEndHour, nowMs)) return false
        if (s.dailyLimit > 0 && todayAnsweredCount >= s.dailyLimit) return false

        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
        // Guard against duplicate SCREEN_ON + USER_PRESENT bursts within 4 seconds.
        if (nowMs - lastShownAt < 4_000L) return false

        return when (s.triggerMode) {
            TriggerMode.EVERY_UNLOCK -> true
            TriggerMode.EVERY_N_UNLOCKS -> {
                val nextCount = prefs.getInt(KEY_UNLOCK_COUNTER, 0) + 1
                prefs.edit().putInt(KEY_UNLOCK_COUNTER, nextCount).apply()
                nextCount % s.everyNUnlocks == 0
            }
            TriggerMode.COOLDOWN_MINUTES -> {
                nowMs - lastShownAt >= s.cooldownMinutes * 60_000L
            }
        }
    }

    private fun isWithinQuietHours(startHour: Int, endHour: Int, nowMs: Long): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        return if (startHour <= endHour) {
            hour in startHour until endHour
        } else {
            hour >= startHour || hour < endHour
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T =
        try {
            if (raw.isNullOrBlank()) fallback else enumValueOf<T>(raw)
        } catch (_: IllegalArgumentException) {
            fallback
        }

    companion object {
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_TRIGGER_MODE = "trigger_mode"
        private const val KEY_EVERY_N = "every_n_unlocks"
        private const val KEY_COOLDOWN_MINUTES = "cooldown_minutes"
        private const val KEY_SELECTION_POLICY = "selection_policy"
        private const val KEY_FILTER_POS = "filter_pos"
        private const val KEY_FILTER_CEFR = "filter_cefr"
        private const val KEY_QUIET_ENABLED = "quiet_enabled"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_LAST_SHOWN_AT = "last_shown_at"
        private const val KEY_UNLOCK_COUNTER = "unlock_counter"
        private const val KEY_LAST_WORD_ID = "last_word_id"
    }
}
