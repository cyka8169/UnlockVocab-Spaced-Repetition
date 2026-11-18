package com.huy.unlockvocab.data

import android.content.Context
import android.util.Log
import com.huy.unlockvocab.data.db.AppDatabase
import com.huy.unlockvocab.data.db.DictionaryEntry
import com.huy.unlockvocab.data.db.QuizOption
import com.huy.unlockvocab.data.db.QuizQuestion
import com.huy.unlockvocab.data.db.ReviewLogEntity
import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.StatsSummary
import com.huy.unlockvocab.data.db.WordEntity
import com.huy.unlockvocab.data.prefs.SettingsStore
import com.huy.unlockvocab.domain.Sm2Scheduler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class VocabRepository private constructor(private val appContext: Context) {
    val db = AppDatabase(appContext)
    val settingsStore = SettingsStore(appContext)

    fun ensureSeeded() {
        synchronized(seedLock) {
            if (db.getStudyWordCount() == 0) {
                val seedJson = appContext.assets.open("vocab_seed.json").bufferedReader().use { it.readText() }
                db.insertSeedWords(seedJson)
                Log.d(TAG, "Seeded study_words: ${db.getStudyWordCount()}")
            }
            if (db.getDictEntryCount() == 0) {
                val dictJson = appContext.assets.open("dict_compact.json").bufferedReader().use { it.readText() }
                db.insertDictEntries(dictJson)
                Log.d(TAG, "Seeded dict_entries: ${db.getDictEntryCount()}")
            }
        }
    }

    fun buildNextQuizQuestion(): QuizQuestion? {
        ensureSeeded()
        val s = settingsStore.load()
        val now = System.currentTimeMillis()
        val target = db.selectCandidateWord(
            policy = s.selectionPolicy,
            filterPos = s.filterPos,
            filterCefr = s.filterCefr,
            excludeWordId = settingsStore.getLastWordId(),
            nowMs = now
        ) ?: return null

        val distractors = db.getDistractorMeanings(target, 3)
        val options = (listOf(QuizOption(target.primaryMeaning, isCorrect = true)) +
            distractors.map { QuizOption(it, isCorrect = false) }).shuffled()
        settingsStore.setLastWordId(target.id)
        return QuizQuestion(word = target, options = options)
    }

    fun recordOutcome(wordId: Long, outcome: ReviewOutcome, responseTimeMs: Long) {
        val current = db.getWordById(wordId) ?: return
        val now = System.currentTimeMillis()
        val updated = Sm2Scheduler.applyReview(current, outcome, responseTimeMs, now)
        if (outcome != ReviewOutcome.DISMISSED) {
            db.updateWordSm2(updated)
        }
        db.insertReviewLog(current, outcome, responseTimeMs, now)
    }

    fun getTodayAnsweredCount(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return db.getTodayAnsweredCount(cal.timeInMillis)
    }

    fun searchDictionary(query: String): List<DictionaryEntry> {
        ensureSeeded()
        return db.searchDictionaryPrefix(query, limit = 15)
    }

    fun addOrUpdateCustomWord(
        word: String,
        pos: String,
        ipa: String,
        cefr: String,
        topic: String,
        primaryMeaning: String,
        extraMeanings: List<String>,
        example: String,
        isCustom: Boolean
    ): Long {
        ensureSeeded()
        return db.upsertStudyWord(
            word = word,
            pos = pos,
            ipa = ipa,
            cefr = cefr,
            topic = topic,
            primaryMeaning = primaryMeaning,
            extraMeanings = extraMeanings,
            example = example,
            isCustom = isCustom
        )
    }

    fun getStudyWords(query: String = "", posFilter: String = "ALL", customOnly: Boolean = false): List<WordEntity> {
        ensureSeeded()
        return db.getStudyWords(query, posFilter, customOnly)
    }

    fun deleteWord(id: Long) = db.deleteStudyWord(id)

    fun getStatsSummary(): StatsSummary {
        ensureSeeded()
        return db.getStatsSummary()
    }

    fun getRecentLogs(): List<ReviewLogEntity> = db.getRecentLogs(limit = 50)

    fun exportLogsToCsv(): File {
        val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val outFile = File(dir, "unlockvocab_review_logs.csv")
        val logs = db.getAllLogsChronological()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("id,word_id,word,pos,outcome,response_time_ms,timestamp_ms,datetime\n")
            for (log in logs) {
                val safeWord = log.word.replace("\"", "\"\"")
                val dt = sdf.format(Date(log.timestamp))
                w.write("${log.id},${log.wordId},\"$safeWord\",${log.pos},${log.outcome.name},${log.responseTimeMs},${log.timestamp},\"$dt\"\n")
            }
        }
        return outFile
    }

    companion object {
        private const val TAG = "UnlockVocab"
        private val seedLock = Any()

        @Volatile
        private var instance: VocabRepository? = null

        fun getInstance(context: Context): VocabRepository =
            instance ?: synchronized(this) {
                instance ?: VocabRepository(context.applicationContext).also { instance = it }
            }
    }
}
