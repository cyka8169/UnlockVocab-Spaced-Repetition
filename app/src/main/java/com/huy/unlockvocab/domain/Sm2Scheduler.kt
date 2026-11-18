package com.huy.unlockvocab.domain

import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.WordEntity
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * SuperMemo-2 (SM-2) scheduler adapted with response-time quality grading.
 * DISMISSED reviews leave all SM-2 scheduling parameters untouched.
 */
object Sm2Scheduler {
    private const val DAY_MS = 86_400_000L
    private const val TEN_MINUTES_MS = 600_000L

    fun applyReview(
        word: WordEntity,
        outcome: ReviewOutcome,
        responseTimeMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): WordEntity {
        if (outcome == ReviewOutcome.DISMISSED) {
            return word
        }

        if (outcome == ReviewOutcome.WRONG) {
            return word.copy(
                repetitions = 0,
                intervalDays = 0,
                easeFactor = max(1.3, word.easeFactor - 0.20),
                nextReviewAt = nowMs + TEN_MINUTES_MS,
                lapses = word.lapses + 1,
                lastResponseMs = responseTimeMs,
                totalWrong = word.totalWrong + 1
            )
        }

        val quality = when {
            responseTimeMs < 2_500L -> 5
            responseTimeMs < 6_000L -> 4
            else -> 3
        }

        val newReps = word.repetitions + 1
        val newIntervalDays = when (word.repetitions) {
            0 -> 1
            1 -> 6
            else -> max(1, (word.intervalDays * word.easeFactor).roundToInt())
        }

        val delta = 0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)
        val newEase = max(1.3, word.easeFactor + delta)

        return word.copy(
            repetitions = newReps,
            intervalDays = newIntervalDays,
            easeFactor = newEase,
            nextReviewAt = nowMs + newIntervalDays * DAY_MS,
            lastResponseMs = responseTimeMs,
            totalCorrect = word.totalCorrect + 1
        )
    }
}
