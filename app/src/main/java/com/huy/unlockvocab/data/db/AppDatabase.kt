package com.huy.unlockvocab.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AppDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_STUDY_WORDS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word TEXT NOT NULL,
                pos TEXT NOT NULL,
                ipa TEXT NOT NULL DEFAULT '',
                cefr TEXT NOT NULL DEFAULT '',
                topic TEXT NOT NULL DEFAULT '',
                primary_meaning TEXT NOT NULL,
                extra_meanings TEXT NOT NULL DEFAULT '[]',
                example TEXT NOT NULL DEFAULT '',
                is_custom INTEGER NOT NULL DEFAULT 0,
                repetitions INTEGER NOT NULL DEFAULT 0,
                interval_days INTEGER NOT NULL DEFAULT 0,
                ease_factor REAL NOT NULL DEFAULT 2.5,
                next_review_at INTEGER NOT NULL DEFAULT 0,
                lapses INTEGER NOT NULL DEFAULT 0,
                last_response_ms INTEGER NOT NULL DEFAULT 0,
                total_correct INTEGER NOT NULL DEFAULT 0,
                total_wrong INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                UNIQUE(word, pos)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_study_due ON $TABLE_STUDY_WORDS(next_review_at, pos)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DICT_ENTRIES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word TEXT NOT NULL,
                pos TEXT NOT NULL,
                ipa TEXT NOT NULL DEFAULT '',
                cefr TEXT NOT NULL DEFAULT '',
                meanings TEXT NOT NULL DEFAULT '[]',
                example TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_dict_word ON $TABLE_DICT_ENTRIES(word)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_REVIEW_LOGS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word_id INTEGER NOT NULL,
                word TEXT NOT NULL,
                pos TEXT NOT NULL,
                outcome TEXT NOT NULL,
                response_time_ms INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_logs_ts ON $TABLE_REVIEW_LOGS(timestamp DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Initial schema version 1.
    }

    fun getStudyWordCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_STUDY_WORDS", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    fun getDictEntryCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $TABLE_DICT_ENTRIES", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    fun insertSeedWords(jsonText: String) {
        val arr = JSONArray(jsonText)
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val extras = obj.optJSONArray("extraMeanings")?.toString() ?: "[]"
                val cv = ContentValues().apply {
                    put("word", obj.getString("word").trim().lowercase())
                    put("pos", obj.getString("pos").trim().lowercase())
                    put("ipa", obj.optString("ipa", ""))
                    put("cefr", obj.optString("cefr", ""))
                    put("topic", obj.optString("topic", ""))
                    put("primary_meaning", obj.getString("primaryMeaning").trim())
                    put("extra_meanings", extras)
                    put("example", obj.optString("example", ""))
                    put("is_custom", 0)
                    put("created_at", now)
                }
                db.insertWithOnConflict(TABLE_STUDY_WORDS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun insertDictEntries(jsonText: String) {
        val arr = JSONArray(jsonText)
        val db = writableDatabase
        db.beginTransaction()
        try {
            val stmt = db.compileStatement(
                "INSERT INTO $TABLE_DICT_ENTRIES (word, pos, ipa, cefr, meanings, example) VALUES (?, ?, ?, ?, ?, ?)"
            )
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                stmt.clearBindings()
                stmt.bindString(1, obj.getString("word").trim().lowercase())
                stmt.bindString(2, obj.getString("pos").trim().lowercase())
                stmt.bindString(3, obj.optString("ipa", ""))
                stmt.bindString(4, obj.optString("cefr", ""))
                stmt.bindString(5, obj.optJSONArray("meanings")?.toString() ?: "[]")
                stmt.bindString(6, obj.optString("example", ""))
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun searchDictionaryPrefix(query: String, limit: Int = 15): List<DictionaryEntry> {
        val clean = query.trim().lowercase()
        if (clean.isEmpty()) return emptyList()
        val escaped = clean.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
        val results = mutableListOf<DictionaryEntry>()
        val sql = """
            SELECT id, word, pos, ipa, cefr, meanings, example
            FROM $TABLE_DICT_ENTRIES
            WHERE word LIKE ? ESCAPE '\'
            ORDER BY CASE WHEN word = ? THEN 0 ELSE 1 END, LENGTH(word) ASC, word ASC
            LIMIT ?
        """.trimIndent()
        readableDatabase.rawQuery(sql, arrayOf("$escaped%", clean, limit.toString())).use { c ->
            while (c.moveToNext()) {
                results.add(c.toDictEntry())
            }
        }
        return results
    }

    fun upsertStudyWord(
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
        val cleanWord = word.trim().lowercase()
        val cleanPos = PosTag.fromCode(pos).code
        val cleanPrimary = primaryMeaning.trim()
        val cleanExtras = extraMeanings.map { it.trim() }.filter { it.isNotEmpty() && it != cleanPrimary }.distinct()
        val extrasJson = JSONArray(cleanExtras).toString()

        val db = writableDatabase
        val targetId = editingWordId ?: db.rawQuery(
            "SELECT id FROM $TABLE_STUDY_WORDS WHERE word = ? AND pos = ? LIMIT 1",
            arrayOf(cleanWord, cleanPos)
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else -1L }

        return if (targetId > 0) {
            val cv = ContentValues().apply {
                put("word", cleanWord)
                put("pos", cleanPos)
                put("ipa", ipa.trim())
                put("cefr", cefr.trim())
                put("topic", topic.trim())
                put("primary_meaning", cleanPrimary)
                put("extra_meanings", extrasJson)
                put("example", example.trim())
                if (editingWordId == null) {
                    put("is_custom", if (isCustom) 1 else 0)
                }
            }
            // If renaming word+pos hits another row, delete the duplicate target or ignore gracefully.
            try {
                db.update(TABLE_STUDY_WORDS, cv, "id = ?", arrayOf(targetId.toString()))
                targetId
            } catch (_: Exception) {
                -1L
            }
        } else {
            val cv = ContentValues().apply {
                put("word", cleanWord)
                put("pos", cleanPos)
                put("ipa", ipa.trim())
                put("cefr", cefr.trim())
                put("topic", topic.trim())
                put("primary_meaning", cleanPrimary)
                put("extra_meanings", extrasJson)
                put("example", example.trim())
                put("is_custom", if (isCustom) 1 else 0)
                put("created_at", System.currentTimeMillis())
            }
            db.insert(TABLE_STUDY_WORDS, null, cv)
        }
    }

    fun importTopicPackFromDict(packCode: String): Int {
        val (whereClause, topicLabel, limit) = when (packCode) {
            "IELTS_B1_B2" -> Triple(
                "cefr IN ('B1', 'B2') AND pos IN ('noun', 'verb', 'adj', 'adv')",
                "Pack-B1-B2",
                100
            )
            else -> Triple(
                "pos IN ('verb', 'adj') AND LENGTH(word) >= 6",
                "Pack-Academic",
                80
            )
        }
        val beforeCount = getStudyWordCount()
        val db = writableDatabase
        val now = System.currentTimeMillis()
        val sql = """
            SELECT d.word, d.pos, d.ipa, d.cefr, d.meanings, d.example
            FROM $TABLE_DICT_ENTRIES d
            LEFT JOIN $TABLE_STUDY_WORDS s ON d.word = s.word AND d.pos = s.pos
            WHERE s.id IS NULL AND $whereClause
            ORDER BY RANDOM()
            LIMIT ?
        """.trimIndent()

        val rowsToInsert = mutableListOf<DictionaryEntry>()
        readableDatabase.rawQuery(sql, arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) {
                val meanings = parseJsonArray(c.getString(4))
                if (meanings.isNotEmpty()) {
                    rowsToInsert.add(
                        DictionaryEntry(
                            word = c.getString(0),
                            pos = c.getString(1),
                            ipa = c.getString(2),
                            cefr = c.getString(3),
                            meanings = meanings,
                            example = c.getString(5)
                        )
                    )
                }
            }
        }

        db.beginTransaction()
        try {
            for (item in rowsToInsert) {
                val primary = item.meanings.first()
                val extras = JSONArray(item.meanings.drop(1)).toString()
                val cv = ContentValues().apply {
                    put("word", item.word)
                    put("pos", item.pos)
                    put("ipa", item.ipa)
                    put("cefr", item.cefr)
                    put("topic", topicLabel)
                    put("primary_meaning", primary)
                    put("extra_meanings", extras)
                    put("example", item.example)
                    put("is_custom", 0)
                    put("created_at", now)
                }
                db.insertWithOnConflict(TABLE_STUDY_WORDS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return (getStudyWordCount() - beforeCount).coerceAtLeast(0)
    }

    fun importBulkCustomWords(items: List<Triple<String, String, List<String>>>): Int {
        val db = writableDatabase
        val now = System.currentTimeMillis()
        var added = 0
        db.beginTransaction()
        try {
            for ((rawWord, rawPos, meanings) in items) {
                val cleanWord = rawWord.trim().lowercase()
                val cleanPos = PosTag.fromCode(rawPos).code
                val cleanPrimary = meanings.firstOrNull()?.trim().orEmpty()
                if (cleanWord.isEmpty() || cleanPrimary.isEmpty()) continue
                val cleanExtras = meanings.drop(1).map { it.trim() }.filter { it.isNotEmpty() && it != cleanPrimary }.distinct()
                val cv = ContentValues().apply {
                    put("word", cleanWord)
                    put("pos", cleanPos)
                    put("ipa", "")
                    put("cefr", "")
                    put("topic", "BulkImport")
                    put("primary_meaning", cleanPrimary)
                    put("extra_meanings", JSONArray(cleanExtras).toString())
                    put("example", "")
                    put("is_custom", 1)
                    put("created_at", now)
                }
                val rowId = db.insertWithOnConflict(TABLE_STUDY_WORDS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                if (rowId > 0) added++
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return added
    }

    fun getAllStudyWordsForExport(): List<WordEntity> {
        val list = mutableListOf<WordEntity>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_STUDY_WORDS ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) list.add(c.toWordEntity())
        }
        return list
    }

    fun updateWordSm2(word: WordEntity) {
        val cv = ContentValues().apply {
            put("repetitions", word.repetitions)
            put("interval_days", word.intervalDays)
            put("ease_factor", word.easeFactor)
            put("next_review_at", word.nextReviewAt)
            put("lapses", word.lapses)
            put("last_response_ms", word.lastResponseMs)
            put("total_correct", word.totalCorrect)
            put("total_wrong", word.totalWrong)
        }
        writableDatabase.update(TABLE_STUDY_WORDS, cv, "id = ?", arrayOf(word.id.toString()))
    }

    fun deleteStudyWord(id: Long) {
        writableDatabase.delete(TABLE_STUDY_WORDS, "id = ?", arrayOf(id.toString()))
    }

    fun getWordById(id: Long): WordEntity? =
        readableDatabase.rawQuery("SELECT * FROM $TABLE_STUDY_WORDS WHERE id = ? LIMIT 1", arrayOf(id.toString()))
            .use { c -> if (c.moveToFirst()) c.toWordEntity() else null }

    fun getStudyWords(query: String = "", posFilter: String = "ALL", customOnly: Boolean = false, limit: Int = 200): List<WordEntity> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isNotEmpty()) {
            where.add("(word LIKE ? OR primary_meaning LIKE ?)")
            args.add("%$cleanQuery%")
            args.add("%$cleanQuery%")
        }
        if (posFilter != "ALL") {
            where.add("pos = ?")
            args.add(posFilter)
        }
        if (customOnly) {
            where.add("is_custom = 1")
        }
        val whereClause = if (where.isEmpty()) "" else "WHERE " + where.joinToString(" AND ")
        val sql = "SELECT * FROM $TABLE_STUDY_WORDS $whereClause ORDER BY is_custom DESC, next_review_at ASC, id DESC LIMIT $limit"
        val list = mutableListOf<WordEntity>()
        readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            while (c.moveToNext()) list.add(c.toWordEntity())
        }
        return list
    }

    fun selectCandidateWord(
        policy: WordSelectionPolicy,
        filterPos: String,
        filterCefr: String,
        excludeWordId: Long,
        nowMs: Long
    ): WordEntity? {
        val db = readableDatabase

        fun queryFirst(sql: String, args: Array<String>): WordEntity? =
            db.rawQuery(sql, args).use { c -> if (c.moveToFirst()) c.toWordEntity() else null }

        val notSameGuard = "id != ?"
        val exArg = excludeWordId.toString()

        val selected = when (policy) {
            WordSelectionPolicy.DUE_FIRST -> {
                queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard AND next_review_at > 0 AND next_review_at <= ? ORDER BY next_review_at ASC LIMIT 1",
                    arrayOf(exArg, nowMs.toString())
                ) ?: queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard AND repetitions = 0 ORDER BY is_custom DESC, RANDOM() LIMIT 1",
                    arrayOf(exArg)
                ) ?: queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard ORDER BY next_review_at ASC LIMIT 1",
                    arrayOf(exArg)
                )
            }

            WordSelectionPolicy.NEW_WORDS -> {
                queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard AND repetitions = 0 AND total_wrong = 0 ORDER BY is_custom DESC, RANDOM() LIMIT 1",
                    arrayOf(exArg)
                ) ?: queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard ORDER BY repetitions ASC, RANDOM() LIMIT 1",
                    arrayOf(exArg)
                )
            }

            WordSelectionPolicy.HARDEST_FIRST -> {
                queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard AND (lapses > 0 OR total_wrong > 0) ORDER BY lapses DESC, total_wrong DESC, ease_factor ASC, last_response_ms DESC LIMIT 1",
                    arrayOf(exArg)
                ) ?: queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard ORDER BY ease_factor ASC, RANDOM() LIMIT 1",
                    arrayOf(exArg)
                )
            }

            WordSelectionPolicy.BY_TOPIC_OR_POS -> {
                val clauses = mutableListOf(notSameGuard)
                val args = mutableListOf(exArg)
                if (filterPos != "ALL") {
                    clauses.add("pos = ?")
                    args.add(filterPos)
                }
                if (filterCefr != "ALL") {
                    clauses.add("cefr = ?")
                    args.add(filterCefr)
                }
                val where = clauses.joinToString(" AND ")
                queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $where AND next_review_at > 0 AND next_review_at <= ? ORDER BY next_review_at ASC LIMIT 1",
                    (args + nowMs.toString()).toTypedArray()
                ) ?: queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $where ORDER BY repetitions ASC, RANDOM() LIMIT 1",
                    args.toTypedArray()
                )
            }

            WordSelectionPolicy.RANDOM_MIXED -> {
                queryFirst(
                    "SELECT * FROM $TABLE_STUDY_WORDS WHERE $notSameGuard ORDER BY RANDOM() LIMIT 1",
                    arrayOf(exArg)
                )
            }
        }

        return selected ?: queryFirst("SELECT * FROM $TABLE_STUDY_WORDS ORDER BY RANDOM() LIMIT 1", emptyArray())
    }

    fun getDistractorMeanings(target: WordEntity, count: Int = 3): List<String> =
        getDistractorValues(target, isEnglishAnswer = false, count = count)

    fun getDistractorEnglishWords(target: WordEntity, count: Int = 3): List<String> =
        getDistractorValues(target, isEnglishAnswer = true, count = count)

    private fun getDistractorValues(target: WordEntity, isEnglishAnswer: Boolean, count: Int): List<String> {
        val chosen = linkedSetOf<String>()
        val db = readableDatabase
        val studyCol = if (isEnglishAnswer) "word" else "primary_meaning"
        val targetVal = if (isEnglishAnswer) target.word else target.primaryMeaning

        // 1) Prefer distractors from study_words with identical POS.
        db.rawQuery(
            "SELECT DISTINCT $studyCol FROM $TABLE_STUDY_WORDS WHERE pos = ? AND id != ? AND $studyCol != ? ORDER BY RANDOM() LIMIT ?",
            arrayOf(target.pos, target.id.toString(), targetVal, (count * 2).toString())
        ).use { c ->
            while (c.moveToNext() && chosen.size < count) {
                val v = c.getString(0).trim()
                if (v.isNotEmpty()) chosen.add(v)
            }
        }

        // 2) Fallback to dict_entries with identical POS.
        if (chosen.size < count) {
            val dictCol = if (isEnglishAnswer) "word" else "meanings"
            db.rawQuery(
                "SELECT $dictCol FROM $TABLE_DICT_ENTRIES WHERE pos = ? AND word != ? ORDER BY RANDOM() LIMIT 20",
                arrayOf(target.pos, target.word)
            ).use { c ->
                while (c.moveToNext() && chosen.size < count) {
                    val v = if (isEnglishAnswer) {
                        c.getString(0).trim()
                    } else {
                        parseJsonArray(c.getString(0)).firstOrNull()?.trim().orEmpty()
                    }
                    if (v.isNotEmpty() && !v.equals(targetVal, ignoreCase = true)) {
                        chosen.add(v)
                    }
                }
            }
        }

        // 3) Last resort: any other study_words entry.
        if (chosen.size < count) {
            db.rawQuery(
                "SELECT DISTINCT $studyCol FROM $TABLE_STUDY_WORDS WHERE id != ? AND $studyCol != ? ORDER BY RANDOM() LIMIT ?",
                arrayOf(target.id.toString(), targetVal, (count * 2).toString())
            ).use { c ->
                while (c.moveToNext() && chosen.size < count) {
                    val v = c.getString(0).trim()
                    if (v.isNotEmpty()) chosen.add(v)
                }
            }
        }

        return chosen.take(count)
    }

    fun insertReviewLog(word: WordEntity, outcome: ReviewOutcome, responseTimeMs: Long, timestamp: Long) {
        val cv = ContentValues().apply {
            put("word_id", word.id)
            put("word", word.word)
            put("pos", word.pos)
            put("outcome", outcome.name)
            put("response_time_ms", responseTimeMs)
            put("timestamp", timestamp)
        }
        writableDatabase.insert(TABLE_REVIEW_LOGS, null, cv)
    }

    fun getTodayAnsweredCount(dayStartMs: Long): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_REVIEW_LOGS WHERE timestamp >= ? AND outcome != ?",
            arrayOf(dayStartMs.toString(), ReviewOutcome.DISMISSED.name)
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    fun getRecentLogs(limit: Int = 50): List<ReviewLogEntity> {
        val list = mutableListOf<ReviewLogEntity>()
        readableDatabase.rawQuery(
            "SELECT id, word_id, word, pos, outcome, response_time_ms, timestamp FROM $TABLE_REVIEW_LOGS ORDER BY timestamp DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ReviewLogEntity(
                        id = c.getLong(0),
                        wordId = c.getLong(1),
                        word = c.getString(2),
                        pos = c.getString(3),
                        outcome = ReviewOutcome.valueOf(c.getString(4)),
                        responseTimeMs = c.getLong(5),
                        timestamp = c.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun getAllLogsChronological(): List<ReviewLogEntity> {
        val list = mutableListOf<ReviewLogEntity>()
        readableDatabase.rawQuery(
            "SELECT id, word_id, word, pos, outcome, response_time_ms, timestamp FROM $TABLE_REVIEW_LOGS ORDER BY timestamp ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ReviewLogEntity(
                        id = c.getLong(0),
                        wordId = c.getLong(1),
                        word = c.getString(2),
                        pos = c.getString(3),
                        outcome = ReviewOutcome.valueOf(c.getString(4)),
                        responseTimeMs = c.getLong(5),
                        timestamp = c.getLong(6)
                    )
                )
            }
        }
        return list
    }

    fun getStatsSummary(nowMs: Long = System.currentTimeMillis()): StatsSummary {
        val db = readableDatabase
        val totalWords = getStudyWordCount()
        val studied = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_STUDY_WORDS WHERE repetitions > 0 OR total_wrong > 0",
            null
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        val mastered = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_STUDY_WORDS WHERE repetitions >= 2",
            null
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        val learning = (studied - mastered).coerceAtLeast(0)
        val unseen = (totalWords - studied).coerceAtLeast(0)
        val due = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_STUDY_WORDS WHERE next_review_at > 0 AND next_review_at <= ?",
            arrayOf(nowMs.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
        val custom = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_STUDY_WORDS WHERE is_custom = 1",
            null
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

        var totalReviews = 0
        var correct = 0
        var wrong = 0
        var dismissed = 0
        db.rawQuery("SELECT outcome, COUNT(*) FROM $TABLE_REVIEW_LOGS GROUP BY outcome", null).use { c ->
            while (c.moveToNext()) {
                val count = c.getInt(1)
                totalReviews += count
                when (c.getString(0)) {
                    ReviewOutcome.CORRECT.name -> correct = count
                    ReviewOutcome.WRONG.name -> wrong = count
                    ReviewOutcome.DISMISSED.name -> dismissed = count
                }
            }
        }

        val avgMs = db.rawQuery(
            "SELECT AVG(response_time_ms) FROM $TABLE_REVIEW_LOGS WHERE outcome != ?",
            arrayOf(ReviewOutcome.DISMISSED.name)
        ).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }

        val hardest = mutableListOf<WordEntity>()
        db.rawQuery(
            "SELECT * FROM $TABLE_STUDY_WORDS WHERE lapses > 0 OR total_wrong > 0 ORDER BY lapses DESC, total_wrong DESC, ease_factor ASC LIMIT 5",
            null
        ).use { c ->
            while (c.moveToNext()) hardest.add(c.toWordEntity())
        }

        val last7Days = computeLast7DaysStats(db, nowMs)

        return StatsSummary(
            totalWords = totalWords,
            studiedWords = studied,
            dueWords = due,
            customWords = custom,
            unseenWords = unseen,
            learningWords = learning,
            masteredWords = mastered,
            totalReviews = totalReviews,
            correctReviews = correct,
            wrongReviews = wrong,
            dismissedReviews = dismissed,
            avgResponseMs = avgMs,
            hardestWords = hardest,
            last7Days = last7Days
        )
    }

    private fun computeLast7DaysStats(db: SQLiteDatabase, nowMs: Long): List<DailyStatPoint> {
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val startMs = cal.timeInMillis
        val countsByKey = linkedMapOf<String, IntArray>()
        val labelsByKey = linkedMapOf<String, String>()
        for (i in 0 until 7) {
            val d = cal.time
            val k = keyFmt.format(d)
            countsByKey[k] = intArrayOf(0, 0, 0)
            labelsByKey[k] = labelFmt.format(d)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        db.rawQuery(
            "SELECT outcome, timestamp FROM $TABLE_REVIEW_LOGS WHERE timestamp >= ?",
            arrayOf(startMs.toString())
        ).use { c ->
            while (c.moveToNext()) {
                val outcome = c.getString(0)
                val ts = c.getLong(1)
                val k = keyFmt.format(Date(ts))
                val arr = countsByKey[k] ?: continue
                when (outcome) {
                    ReviewOutcome.CORRECT.name -> arr[0]++
                    ReviewOutcome.WRONG.name -> arr[1]++
                    ReviewOutcome.DISMISSED.name -> arr[2]++
                }
            }
        }

        return countsByKey.map { (k, arr) ->
            DailyStatPoint(
                dateLabel = labelsByKey[k] ?: k,
                correct = arr[0],
                wrong = arr[1],
                dismissed = arr[2]
            )
        }
    }

    private fun Cursor.toWordEntity(): WordEntity = WordEntity(
        id = getLong(getColumnIndexOrThrow("id")),
        word = getString(getColumnIndexOrThrow("word")),
        pos = getString(getColumnIndexOrThrow("pos")),
        ipa = getString(getColumnIndexOrThrow("ipa")),
        cefr = getString(getColumnIndexOrThrow("cefr")),
        topic = getString(getColumnIndexOrThrow("topic")),
        primaryMeaning = getString(getColumnIndexOrThrow("primary_meaning")),
        extraMeanings = parseJsonArray(getString(getColumnIndexOrThrow("extra_meanings"))),
        example = getString(getColumnIndexOrThrow("example")),
        isCustom = getInt(getColumnIndexOrThrow("is_custom")) == 1,
        repetitions = getInt(getColumnIndexOrThrow("repetitions")),
        intervalDays = getInt(getColumnIndexOrThrow("interval_days")),
        easeFactor = getDouble(getColumnIndexOrThrow("ease_factor")),
        nextReviewAt = getLong(getColumnIndexOrThrow("next_review_at")),
        lapses = getInt(getColumnIndexOrThrow("lapses")),
        lastResponseMs = getLong(getColumnIndexOrThrow("last_response_ms")),
        totalCorrect = getInt(getColumnIndexOrThrow("total_correct")),
        totalWrong = getInt(getColumnIndexOrThrow("total_wrong")),
        createdAt = getLong(getColumnIndexOrThrow("created_at"))
    )

    private fun Cursor.toDictEntry(): DictionaryEntry = DictionaryEntry(
        id = getLong(getColumnIndexOrThrow("id")),
        word = getString(getColumnIndexOrThrow("word")),
        pos = getString(getColumnIndexOrThrow("pos")),
        ipa = getString(getColumnIndexOrThrow("ipa")),
        cefr = getString(getColumnIndexOrThrow("cefr")),
        meanings = parseJsonArray(getString(getColumnIndexOrThrow("meanings"))),
        example = getString(getColumnIndexOrThrow("example"))
    )

    private fun parseJsonArray(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { idx -> arr.optString(idx) }.filter { it.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val DB_NAME = "unlock_vocab.db"
        private const val DB_VERSION = 1
        private const val TABLE_STUDY_WORDS = "study_words"
        private const val TABLE_DICT_ENTRIES = "dict_entries"
        private const val TABLE_REVIEW_LOGS = "review_logs"
    }
}
