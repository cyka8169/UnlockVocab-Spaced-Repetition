package com.huy.unlockvocab.data

import android.content.Context
import android.util.Log
import com.huy.unlockvocab.data.db.AppDatabase
import com.huy.unlockvocab.data.db.DictionaryEntry
import com.huy.unlockvocab.data.db.QuizOption
import com.huy.unlockvocab.data.db.QuizQuestion
import com.huy.unlockvocab.data.db.QuizQuestionStyle
import com.huy.unlockvocab.data.db.ResolvedPromptType
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
            val needSeedWords = db.getStudyWordCount() == 0
            val needRefreshDict = db.getDictEntryCount() == 0
            if (needSeedWords || needRefreshDict) {
                val seedJson = appContext.assets.open("vocab_seed.json").bufferedReader().use { it.readText() }
                db.insertSeedWords(seedJson)
                Log.d(TAG, "Seeded study_words: ${db.getStudyWordCount()}")
            }
            if (needRefreshDict) {
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

        val promptType = when (s.questionStyle) {
            QuizQuestionStyle.EN_TO_VI -> ResolvedPromptType.EN_TO_VI
            QuizQuestionStyle.VI_TO_EN -> ResolvedPromptType.VI_TO_EN
            QuizQuestionStyle.LISTEN_TO_VI -> ResolvedPromptType.LISTEN_TO_VI
            QuizQuestionStyle.AUTO_SM2 -> when {
                target.repetitions <= 1 -> ResolvedPromptType.EN_TO_VI
                target.repetitions == 2 -> ResolvedPromptType.VI_TO_EN
                target.totalCorrect % 2 == 0 -> ResolvedPromptType.LISTEN_TO_VI
                else -> ResolvedPromptType.VI_TO_EN
            }
        }

        val options = if (promptType == ResolvedPromptType.VI_TO_EN) {
            val distractors = db.getDistractorEnglishWords(target, 3)
            (listOf(QuizOption(target.word, isCorrect = true)) +
                distractors.map { QuizOption(it, isCorrect = false) }).shuffled()
        } else {
            val distractors = db.getDistractorMeanings(target, 3)
            (listOf(QuizOption(target.primaryMeaning, isCorrect = true)) +
                distractors.map { QuizOption(it, isCorrect = false) }).shuffled()
        }
        settingsStore.setLastWordId(target.id)
        return QuizQuestion(word = target, options = options, promptType = promptType)
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
        isCustom: Boolean,
        editingWordId: Long? = null
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
            isCustom = isCustom,
            editingWordId = editingWordId
        )
    }

    fun importTopicPack(packCode: String): Int {
        ensureSeeded()
        return db.importTopicPackFromDict(packCode)
    }

    /**
     * Parses lines of format: `word, pos, primaryMeaning; extraMeaning1; extraMeaning2`
     * (supporting either `,` or `|` as column separator).
     */
    fun importBulkCsvLines(rawText: String): Int {
        ensureSeeded()
        val parsed = mutableListOf<Triple<String, String, List<String>>>()
        for (rawLine in rawText.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = if ('|' in line) line.split('|', limit = 3) else line.split(',', limit = 3)
            if (parts.size < 3) continue
            val w = parts[0].trim()
            val pos = parts[1].trim()
            val meanings = parts[2].split(';').map { it.trim() }.filter { it.isNotEmpty() }
            if (w.isNotEmpty() && meanings.isNotEmpty()) {
                parsed.add(Triple(w, pos, meanings))
            }
        }
        if (parsed.isEmpty()) return 0
        return db.importBulkCustomWords(parsed)
    }

    fun getStudyWords(query: String = "", posFilter: String = "ALL", customOnly: Boolean = false): List<WordEntity> {
        ensureSeeded()
        return db.getStudyWords(query, posFilter, customOnly)
    }

    fun getTotalStudyWordCount(): Int {
        ensureSeeded()
        return db.getStudyWordCount()
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

    fun exportWordsToCsv(): File {
        val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        val outFile = File(dir, "unlockvocab_study_words.csv")
        val words = db.getAllStudyWordsForExport()
        outFile.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("id,word,pos,ipa,cefr,topic,primary_meaning,extra_meanings,repetitions,interval_days,ease_factor,total_correct,total_wrong,is_custom\n")
            for (item in words) {
                fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
                w.write(
                    "${item.id},${esc(item.word)},${item.pos},${esc(item.ipa)},${esc(item.cefr)},${esc(item.topic)}," +
                        "${esc(item.primaryMeaning)},${esc(item.extraMeanings.joinToString("; "))}," +
                        "${item.repetitions},${item.intervalDays},${"%.2f".format(Locale.US, item.easeFactor)}," +
                        "${item.totalCorrect},${item.totalWrong},${if (item.isCustom) 1 else 0}\n"
                )
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
