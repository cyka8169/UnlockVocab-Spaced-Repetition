package com.huy.unlockvocab.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.huy.unlockvocab.data.db.AppSettings
import com.huy.unlockvocab.data.db.QuizQuestionStyle
import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.TriggerMode
import com.huy.unlockvocab.data.db.WordSelectionPolicy
import java.util.Calendar
import kotlin.math.ceil

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("unlock_vocab_prefs", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        serviceEnabled = prefs.getBoolean(KEY_SERVICE_ENABLED, true),
        triggerMode = enumValueOrDefault(prefs.getString(KEY_TRIGGER_MODE, null), TriggerMode.EVERY_UNLOCK),
        everyNUnlocks = prefs.getInt(KEY_EVERY_N, 2).coerceIn(1, 20),
        cooldownMinutes = prefs.getInt(KEY_COOLDOWN_MINUTES, 15).coerceIn(1, 180),
        selectionPolicy = enumValueOrDefault(prefs.getString(KEY_SELECTION_POLICY, null), WordSelectionPolicy.DUE_FIRST),
        questionStyle = enumValueOrDefault(prefs.getString(KEY_QUESTION_STYLE, null), QuizQuestionStyle.AUTO_SM2),
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
            .putString(KEY_QUESTION_STYLE, settings.questionStyle.name)
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

    fun pauseForMinutes(minutes: Int, nowMs: Long = System.currentTimeMillis()) {
        val untilMs = nowMs + minutes.coerceAtLeast(1) * 60_000L
        prefs.edit().putLong(KEY_SNOOZE_UNTIL_MS, untilMs).apply()
    }

    fun clearSnooze() {
        prefs.edit()
            .putLong(KEY_SNOOZE_UNTIL_MS, 0L)
            .putInt(KEY_CONSECUTIVE_DISMISS, 0)
            .apply()
    }

    fun getSnoozeRemainingMinutes(nowMs: Long = System.currentTimeMillis()): Int {
        val until = prefs.getLong(KEY_SNOOZE_UNTIL_MS, 0L)
        if (until <= nowMs) return 0
        return ceil((until - nowMs) / 60_000.0).toInt().coerceAtLeast(1)
    }

    fun getSnoozeUntilMs(): Long = prefs.getLong(KEY_SNOOZE_UNTIL_MS, 0L)

    /**
     * Adaptive Snooze: if the user taps outside to dismiss 2+ times within 10 minutes,
     * automatically pause unlock prompts for 15 minutes to avoid annoyance during busy periods.
     */
    fun recordOutcomeForAdaptiveSnooze(outcome: ReviewOutcome, nowMs: Long = System.currentTimeMillis()) {
        if (outcome != ReviewOutcome.DISMISSED) {
            prefs.edit().putInt(KEY_CONSECUTIVE_DISMISS, 0).apply()
            return
        }
        val lastDismissAt = prefs.getLong(KEY_LAST_DISMISS_AT, 0L)
        val prevCount = prefs.getInt(KEY_CONSECUTIVE_DISMISS, 0)
        val nextCount = if (nowMs - lastDismissAt <= 10 * 60_000L) prevCount + 1 else 1
        if (nextCount >= 2) {
            prefs.edit()
                .putInt(KEY_CONSECUTIVE_DISMISS, 0)
                .putLong(KEY_LAST_DISMISS_AT, nowMs)
                .putLong(KEY_SNOOZE_UNTIL_MS, nowMs + ADAPTIVE_SNOOZE_MINUTES * 60_000L)
                .apply()
        } else {
            prefs.edit()
                .putInt(KEY_CONSECUTIVE_DISMISS, nextCount)
                .putLong(KEY_LAST_DISMISS_AT, nowMs)
                .apply()
        }
    }

    /**
     * Evaluates whether the quiz overlay should appear on this unlock event,
     * respecting snooze, quiet hours, active calls, daily quota, and the chosen TriggerMode.
     */
    fun shouldTriggerOnUnlock(
        todayAnsweredCount: Int,
        isInCall: Boolean,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val s = load()
        if (!s.serviceEnabled || isInCall) return false
        if (nowMs < prefs.getLong(KEY_SNOOZE_UNTIL_MS, 0L)) return false
        if (s.quietHoursEnabled && isWithinQuietHours(s.quietStartHour, s.quietEndHour, nowMs)) return false
        if (s.dailyLimit > 0 && todayAnsweredCount >= s.dailyLimit) return false

        val lastShownAt = prefs.getLong(KEY_LAST_SHOWN_AT, 0L)
        // Guard against duplicate unlock callbacks within 700ms while still allowing rapid re-lock tests.
        if (nowMs - lastShownAt < 700L) return false

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
        private const val KEY_QUESTION_STYLE = "question_style"
        private const val KEY_FILTER_POS = "filter_pos"
        private const val KEY_FILTER_CEFR = "filter_cefr"
        private const val KEY_QUIET_ENABLED = "quiet_enabled"
        private const val KEY_QUIET_START = "quiet_start"
        private const val KEY_QUIET_END = "quiet_end"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_LAST_SHOWN_AT = "last_shown_at"
        private const val KEY_UNLOCK_COUNTER = "unlock_counter"
        private const val KEY_LAST_WORD_ID = "last_word_id"
        private const val KEY_SNOOZE_UNTIL_MS = "snooze_until_ms"
        private const val KEY_CONSECUTIVE_DISMISS = "consecutive_dismiss_count"
        private const val KEY_LAST_DISMISS_AT = "last_dismiss_at"
        private const val ADAPTIVE_SNOOZE_MINUTES = 15
    }
}
