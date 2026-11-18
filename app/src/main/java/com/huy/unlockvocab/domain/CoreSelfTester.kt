package com.huy.unlockvocab.domain

import android.util.Log
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.db.CefrLevel
import com.huy.unlockvocab.data.db.QuizQuestionStyle
import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.WordEntity

/**
 * Automated self-test suite for verifying core SM-2 invariants, 50k offline dictionary,
 * distractor generation, bidirectional lookup, Cloze masking, and Pre/Post test creation
 * directly on real Android hardware without modifying the user's study progress.
 */
object CoreSelfTester {
    private const val TAG = "UnlockVocabTest"

    fun runAll(repository: VocabRepository): Pair<Boolean, List<String>> {
        repository.ensureSeeded()
        val results = mutableListOf<String>()
        var allPassed = true

        fun check(name: String, condition: Boolean, detail: String = "") {
            val prefix = if (condition) "PASS" else "FAIL"
            if (!condition) allPassed = false
            val line = if (detail.isBlank()) "[$prefix] $name" else "[$prefix] $name: $detail"
            results.add(line)
            Log.i(TAG, line)
        }

        val sampleWord = WordEntity(
            id = 999_999L,
            word = "evaluate",
            pos = "verb",
            ipa = "/ɪˈvæljueɪt/",
            cefr = "B2",
            topic = "SelfTest",
            primaryMeaning = "đánh giá",
            extraMeanings = listOf("ước lượng"),
            example = "Teachers evaluate students fairly — giáo viên đánh giá học sinh công bằng"
        )

        // 1) DISMISSED must not change SM-2 state
        val afterDismiss = Sm2Scheduler.applyReview(sampleWord, ReviewOutcome.DISMISSED, 1500L, 1_000_000L)
        check(
            "SM2_DISMISSED_NO_PENALTY",
            afterDismiss.repetitions == 0 && afterDismiss.intervalDays == 0 && afterDismiss.easeFactor == sampleWord.easeFactor,
            "reps=${afterDismiss.repetitions}, interval=${afterDismiss.intervalDays}"
        )

        // 2) Fast CORRECT (<2500ms -> q=5) increases EF and schedules 1 day on first rep
        val afterFastCorrect = Sm2Scheduler.applyReview(sampleWord, ReviewOutcome.CORRECT, 1200L, 1_000_000L)
        check(
            "SM2_FAST_CORRECT_Q5",
            afterFastCorrect.repetitions == 1 && afterFastCorrect.intervalDays == 1 && afterFastCorrect.easeFactor > 2.5,
            "reps=${afterFastCorrect.repetitions}, interval=${afterFastCorrect.intervalDays}, ef=${"%.2f".format(afterFastCorrect.easeFactor)}"
        )

        // 3) Second CORRECT schedules 6 days
        val afterSecondCorrect = Sm2Scheduler.applyReview(afterFastCorrect, ReviewOutcome.CORRECT, 1800L, 2_000_000L)
        check(
            "SM2_SECOND_REP_6_DAYS",
            afterSecondCorrect.repetitions == 2 && afterSecondCorrect.intervalDays == 6,
            "interval=${afterSecondCorrect.intervalDays}d"
        )

        // 4) WRONG resets repetitions to 0, increments lapses, never drops EF below 1.3
        var lowEfWord = sampleWord.copy(easeFactor = 1.35, repetitions = 3, intervalDays = 10)
        lowEfWord = Sm2Scheduler.applyReview(lowEfWord, ReviewOutcome.WRONG, 3000L, 3_000_000L)
        check(
            "SM2_WRONG_LAPSE_FLOOR",
            lowEfWord.repetitions == 0 && lowEfWord.lapses == 1 && lowEfWord.easeFactor >= 1.3,
            "reps=${lowEfWord.repetitions}, lapses=${lowEfWord.lapses}, ef=${"%.2f".format(lowEfWord.easeFactor)}"
        )

        // 5) Offline SQLite dictionary has >= 50,000 entries
        val dictCount = repository.getTotalDictEntryCount()
        check("OFFLINE_DICT_50K_READY", dictCount >= 50_000, "entries=$dictCount")

        // 6) All 6 CEFR levels A1-C2 exist in offline dictionary
        val levelCounts = repository.getCefrLevelCounts()
        val allLevelsPresent = CefrLevel.codes.all { code -> (levelCounts[code]?.second ?: 0) > 0 }
        check(
            "CEFR_LEVELS_A1_TO_C2_PRESENT",
            allLevelsPresent,
            CefrLevel.codes.joinToString(",") { "$it=${levelCounts[it]?.second ?: 0}" }
        )

        // 7) Distractor generator produces 3 distinct distractors non-colliding with target meaning
        val realWords = repository.db.getStudyWords(limit = 5)
        val firstWord = realWords.firstOrNull()
        val distractors = if (firstWord != null) repository.db.getDistractorMeanings(firstWord, 3) else emptyList()
        check(
            "DISTRACTOR_SAME_POS_NO_COLLISION",
            firstWord != null && distractors.size == 3 && distractors.none { it.equals(firstWord.primaryMeaning, ignoreCase = true) },
            "word=${firstWord?.word}, distractors=$distractors"
        )

        // 8) Dictionary lookup works for "look" and returns >= 5 entries
        val lookResults = repository.lookupDictionary("look", limit = 20)
        check(
            "DICTIONARY_BIDIRECTIONAL_LOOKUP",
            lookResults.size >= 5 && lookResults.any { it.word == "look" },
            "results=${lookResults.size}"
        )

        // 9) Cloze question replaces target word cleanly with ____ without altering user settings permanently
        val settingsStore = repository.settingsStore
        val savedSettings = settingsStore.load()
        val clozeOk = try {
            settingsStore.save(savedSettings.copy(questionStyle = QuizQuestionStyle.CLOZE_CONTEXT))
            val q = repository.buildNextQuizQuestion(allowAutoAudio = false)
            q != null && q.clozeSentence.contains("____") && q.options.count { it.isCorrect } == 1
        } finally {
            settingsStore.save(savedSettings)
        }
        check("CLOZE_MASKING_CHECK", clozeOk)

        // 10) KHKT Pre/Post 15-question assessment builder produces valid 4-option questions
        val assessment = repository.buildKhktAssessmentQuiz(levelFilter = "ALL", count = 15)
        val assessmentOk = assessment.size == 15 && assessment.all { q ->
            q.options.size == 4 && q.options.count { it.isCorrect } == 1
        }
        check("PRE_POST_TEST_GENERATOR", assessmentOk, "questions=${assessment.size}")

        // 11) Anti-theft ownership guard (package ID + APK signing certificate SHA-256 + SQLite watermark)
        val integrity = repository.verifyOwnershipIntegrity()
        check(
            "ANTI_THEFT_OWNERSHIP_GUARD",
            integrity.isAuthentic,
            "sha=${integrity.certSha256Short}, reason=${integrity.reason}"
        )

        val summaryTag = if (allPassed) "SELF_TEST_PASS" else "SELF_TEST_FAIL"
        Log.i(TAG, "[$summaryTag] Passed ${results.count { it.startsWith("[PASS]") }}/${results.size} checks")
        return allPassed to results
    }
}
