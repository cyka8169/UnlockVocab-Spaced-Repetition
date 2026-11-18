package com.huy.unlockvocab.data.db

enum class PosTag(val code: String, val viLabel: String) {
    NOUN("noun", "Danh từ"),
    VERB("verb", "Động từ"),
    ADJ("adj", "Tính từ"),
    ADV("adv", "Trạng từ"),
    PREP("prep", "Giới từ"),
    PHRASE("phrase", "Cụm từ"),
    OTHER("other", "Khác");

    companion object {
        fun fromCode(code: String): PosTag =
            entries.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) } ?: OTHER
    }
}

enum class TriggerMode(val label: String, val description: String) {
    EVERY_UNLOCK("Mỗi lần mở khóa", "Hiện câu hỏi mỗi lần mở khóa máy (không chờ)"),
    EVERY_N_UNLOCKS("Sau mỗi N lần mở khóa", "Đếm số lần mở khóa rồi mới hiện"),
    COOLDOWN_MINUTES("Cách tối thiểu X phút", "Giữ khoảng nghỉ giữa 2 lần hiện câu hỏi");
}

enum class WordSelectionPolicy(val label: String, val description: String) {
    DUE_FIRST("Đến hạn ôn (SM-2)", "Ưu tiên từ đến lịch ôn tập, sau đó lấy từ mới"),
    NEW_WORDS("Từ mới chưa học", "Ưu tiên từ chưa ôn lần nào"),
    HARDEST_FIRST("Từ hay sai / khó nhất", "Ưu tiên từ có số lần sai cao hoặc ease thấp"),
    BY_TOPIC_OR_POS("Lọc theo từ loại / cấp độ", "Chỉ chọn trong nhóm từ loại hoặc CEFR đã chọn"),
    RANDOM_MIXED("Trộn ngẫu nhiên", "Ôn phản xạ ngẫu nhiên trong toàn bộ từ vựng");
}

enum class ReviewOutcome {
    CORRECT,
    WRONG,
    DISMISSED
}

enum class QuizQuestionStyle(val label: String, val description: String) {
    AUTO_SM2("Tự động theo SM-2", "Mới học: Anh→Việt • Đã quen (lặp ≥ 2): Việt→Anh hoặc Nghe→Việt"),
    EN_TO_VI("Anh → Việt", "Hiện từ tiếng Anh, chọn nghĩa tiếng Việt"),
    VI_TO_EN("Việt → Anh", "Hiện nghĩa tiếng Việt, chọn từ tiếng Anh cùng từ loại"),
    LISTEN_TO_VI("Luyện nghe → Việt", "Tự phát âm (ẩn chữ), chọn nghĩa tiếng Việt")
}

enum class ResolvedPromptType {
    EN_TO_VI,
    VI_TO_EN,
    LISTEN_TO_VI
}

data class WordEntity(
    val id: Long = 0,
    val word: String,
    val pos: String,
    val ipa: String,
    val cefr: String,
    val topic: String,
    val primaryMeaning: String,
    val extraMeanings: List<String>,
    val example: String,
    val isCustom: Boolean = false,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5,
    val nextReviewAt: Long = 0L,
    val lapses: Int = 0,
    val lastResponseMs: Long = 0L,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class DictionaryEntry(
    val id: Long = 0,
    val word: String,
    val pos: String,
    val ipa: String,
    val cefr: String,
    val meanings: List<String>,
    val example: String
)

data class ReviewLogEntity(
    val id: Long = 0,
    val wordId: Long,
    val word: String,
    val pos: String,
    val outcome: ReviewOutcome,
    val responseTimeMs: Long,
    val timestamp: Long
)

data class QuizOption(
    val meaning: String,
    val isCorrect: Boolean
)

data class QuizQuestion(
    val word: WordEntity,
    val options: List<QuizOption>,
    val promptType: ResolvedPromptType = ResolvedPromptType.EN_TO_VI
)

data class DailyStatPoint(
    val dateLabel: String,
    val correct: Int,
    val wrong: Int,
    val dismissed: Int
)

data class StatsSummary(
    val totalWords: Int = 0,
    val studiedWords: Int = 0,
    val dueWords: Int = 0,
    val customWords: Int = 0,
    val unseenWords: Int = 0,
    val learningWords: Int = 0,
    val masteredWords: Int = 0,
    val totalReviews: Int = 0,
    val correctReviews: Int = 0,
    val wrongReviews: Int = 0,
    val dismissedReviews: Int = 0,
    val avgResponseMs: Long = 0L,
    val hardestWords: List<WordEntity> = emptyList(),
    val last7Days: List<DailyStatPoint> = emptyList()
) {
    val retentionPercent: Int
        get() {
            val answered = correctReviews + wrongReviews
            return if (answered == 0) 0 else ((correctReviews * 100.0) / answered).toInt()
        }
}

data class AppSettings(
    val serviceEnabled: Boolean = true,
    val triggerMode: TriggerMode = TriggerMode.EVERY_UNLOCK,
    val everyNUnlocks: Int = 2,
    val cooldownMinutes: Int = 15,
    val selectionPolicy: WordSelectionPolicy = WordSelectionPolicy.DUE_FIRST,
    val questionStyle: QuizQuestionStyle = QuizQuestionStyle.AUTO_SM2,
    val filterPos: String = "ALL",
    val filterCefr: String = "ALL",
    val quietHoursEnabled: Boolean = false,
    val quietStartHour: Int = 23,
    val quietEndHour: Int = 6,
    val dailyLimit: Int = 0
)
