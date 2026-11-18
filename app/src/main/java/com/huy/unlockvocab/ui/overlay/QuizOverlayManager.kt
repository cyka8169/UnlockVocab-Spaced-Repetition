package com.huy.unlockvocab.ui.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.db.QuizQuestion
import com.huy.unlockvocab.data.db.ResolvedPromptType
import com.huy.unlockvocab.data.db.ReviewOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Displays a fast, lightweight WindowManager overlay quiz card on screen unlock.
 * Tapping anywhere outside the 4 answer buttons (and the TTS speaker button)
 * dismisses the overlay immediately and logs DISMISSED without SM-2 penalty.
 */
class QuizOverlayManager(private val context: Context) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val repository = VocabRepository.getInstance(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var rootView: View? = null
    private var currentQuestion: QuizQuestion? = null
    private var shownAtMs: Long = 0L
    private var isAnswered: Boolean = false
    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val res = tts?.setLanguage(Locale.US)
                    ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS init skipped: ${e.message}")
        }
    }

    fun speak(word: String) {
        if (ttsReady) {
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "unlock_vocab_${word}")
        }
    }

    fun showNextQuiz(isManualTest: Boolean = false) {
        if (!Settings.canDrawOverlays(appContext)) {
            Log.w(TAG, "Overlay permission missing, cannot show quiz")
            return
        }
        if (rootView != null) {
            return
        }

        scope.launch {
            val question = withContext(Dispatchers.IO) {
                repository.buildNextQuizQuestion()
            } ?: return@launch

            if (!isManualTest) {
                repository.settingsStore.recordQuizShown()
            }
            displayOverlay(question)
        }
    }

    private fun displayOverlay(question: QuizQuestion) {
        if (rootView != null) return
        currentQuestion = question
        shownAtMs = System.currentTimeMillis()
        isAnswered = false

        val density = appContext.resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val dismissClick = View.OnClickListener {
            dismiss(recordDismiss = !isAnswered)
        }

        val root = FrameLayout(appContext).apply {
            setBackgroundColor(Color.parseColor("#990F172A"))
            isFocusableInTouchMode = true
            requestFocus()
            setOnClickListener(dismissClick)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    dismiss(recordDismiss = !isAnswered)
                    true
                } else {
                    false
                }
            }
        }

        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(22).toFloat()
            }
            elevation = dp(12).toFloat()
            // Tapping anywhere on the card outside the answer buttons also dismisses immediately.
            setOnClickListener(dismissClick)
        }

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
            marginStart = dp(24)
            marginEnd = dp(24)
        }

        // Top badge row: CEFR + topic + quick tap-outside hint
        val topRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setOnClickListener(dismissClick)
        }

        val badgeText = buildString {
            append("UnlockVocab")
            if (question.word.cefr.isNotBlank()) append(" • ${question.word.cefr}")
            if (question.word.isCustom) append(" • Tự thêm")
        }
        val badgeView = TextView(appContext).apply {
            text = badgeText
            setTextColor(Color.parseColor("#1D4ED8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(4), dp(10), dp(4))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#DBEAFE"))
                cornerRadius = dp(99).toFloat()
            }
            setOnClickListener(dismissClick)
        }
        topRow.addView(badgeView)

        val spacer = View(appContext)
        topRow.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))

        val dismissHint = TextView(appContext).apply {
            text = "Chạm ngoài ô để thoát"
            setTextColor(Color.parseColor("#64748B"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setOnClickListener(dismissClick)
        }
        topRow.addView(dismissHint)
        card.addView(topRow)

        // Word + POS + TTS button row
        val wordRow = LinearLayout(appContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, dp(4))
            setOnClickListener(dismissClick)
        }

        val wordColumn = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            setOnClickListener(dismissClick)
        }

        val wordTitle = TextView(appContext).apply {
            text = when (question.promptType) {
                ResolvedPromptType.EN_TO_VI -> "${question.word.word} (${question.word.pos})"
                ResolvedPromptType.VI_TO_EN -> "${question.word.primaryMeaning} (${question.word.pos})"
                ResolvedPromptType.LISTEN_TO_VI -> "Nghe phát âm (${question.word.pos})"
            }
            setTextColor(Color.parseColor("#0F172A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (question.promptType == ResolvedPromptType.VI_TO_EN) 21f else 24f)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener(dismissClick)
        }
        wordColumn.addView(wordTitle)

        val subtitleText = when (question.promptType) {
            ResolvedPromptType.EN_TO_VI, ResolvedPromptType.LISTEN_TO_VI -> question.word.ipa
            ResolvedPromptType.VI_TO_EN -> if (question.word.extraMeanings.isNotEmpty()) {
                "Nghĩa khác: ${question.word.extraMeanings.joinToString(", ")}"
            } else ""
        }
        if (subtitleText.isNotBlank()) {
            val ipaView = TextView(appContext).apply {
                text = subtitleText
                setTextColor(Color.parseColor("#475569"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(2), 0, 0)
                setOnClickListener(dismissClick)
            }
            wordColumn.addView(ipaView)
        }

        wordRow.addView(wordColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Hide speaker before answering in VI_TO_EN mode so audio doesn't spoil the English answer.
        val speakBtn = TextView(appContext).apply {
            text = "Nghe"
            visibility = if (question.promptType == ResolvedPromptType.VI_TO_EN) View.GONE else View.VISIBLE
            setTextColor(Color.parseColor("#1E40AF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EFF6FF"))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), Color.parseColor("#BFDBFE"))
            }
            setOnClickListener {
                speak(question.word.word)
            }
        }
        wordRow.addView(speakBtn)
        card.addView(wordRow)

        val promptView = TextView(appContext).apply {
            text = when (question.promptType) {
                ResolvedPromptType.EN_TO_VI -> "Chọn nghĩa tiếng Việt đúng:"
                ResolvedPromptType.VI_TO_EN -> "Chọn từ tiếng Anh tương ứng:"
                ResolvedPromptType.LISTEN_TO_VI -> "Bấm 'Nghe' để nghe lại và chọn nghĩa đúng:"
            }
            setTextColor(Color.parseColor("#334155"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(4), 0, dp(12))
            setOnClickListener(dismissClick)
        }
        card.addView(promptView)

        val feedbackView = TextView(appContext).apply {
            visibility = View.GONE
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(4), dp(8), dp(4), 0)
            setOnClickListener(dismissClick)
        }

        // Answer buttons A, B, C, D — ONLY these buttons submit answers.
        val labels = listOf("A", "B", "C", "D")
        val optionButtons = mutableListOf<TextView>()

        question.options.forEachIndexed { idx, option ->
            val btn = TextView(appContext).apply {
                val prefix = labels.getOrElse(idx) { "${idx + 1}" }
                text = "$prefix. ${option.meaning}"
                setTextColor(Color.parseColor("#1E293B"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(14), dp(13), dp(14), dp(13))
                background = optionDrawable("#F8FAFC", "#CBD5E1", dp(14), dp(1))
                setOnClickListener {
                    if (isAnswered) return@setOnClickListener
                    isAnswered = true
                    val elapsed = (System.currentTimeMillis() - shownAtMs).coerceAtLeast(100L)

                    question.options.forEachIndexed { i, opt ->
                        val targetBtn = optionButtons[i]
                        when {
                            opt.isCorrect -> {
                                targetBtn.background = optionDrawable("#DCFCE7", "#16A34A", dp(14), dp(2))
                                targetBtn.setTextColor(Color.parseColor("#14532D"))
                            }
                            i == idx && !opt.isCorrect -> {
                                targetBtn.background = optionDrawable("#FEE2E2", "#DC2626", dp(14), dp(2))
                                targetBtn.setTextColor(Color.parseColor("#7F1D1D"))
                            }
                        }
                    }

                    val outcome = if (option.isCorrect) ReviewOutcome.CORRECT else ReviewOutcome.WRONG
                    val correctDisplay = if (question.promptType == ResolvedPromptType.VI_TO_EN) {
                        "${question.word.word} (${question.word.ipa})"
                    } else {
                        "${question.word.word}: ${question.word.primaryMeaning}"
                    }
                    feedbackView.visibility = View.VISIBLE
                    val extraNote = buildString {
                        if (option.isCorrect) {
                            append("Chính xác! ($correctDisplay • ${elapsed} ms)")
                        } else {
                            append("Chưa đúng — Đáp án: $correctDisplay")
                        }
                        if (question.word.extraMeanings.isNotEmpty()) {
                            append("\nNghĩa khác: ${question.word.extraMeanings.joinToString(", ")}")
                        }
                        if (question.word.example.isNotBlank()) {
                            append("\nVD: ${question.word.example}")
                        }
                    }
                    feedbackView.text = extraNote
                    feedbackView.setTextColor(
                        Color.parseColor(if (option.isCorrect) "#15803D" else "#B91C1C")
                    )

                    scope.launch(Dispatchers.IO) {
                        repository.recordOutcome(question.word.id, outcome, elapsed)
                    }

                    mainHandler.postDelayed({ dismiss(recordDismiss = false) }, 1_100L)
                }
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            optionButtons.add(btn)
            card.addView(btn, lp)
        }

        card.addView(feedbackView)
        root.addView(card, cardParams)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(root, layoutParams)
            rootView = root
            if (question.promptType == ResolvedPromptType.LISTEN_TO_VI) {
                mainHandler.postDelayed({ speak(question.word.word) }, 120L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay view: ${e.message}", e)
            rootView = null
            currentQuestion = null
        }
    }

    private fun optionDrawable(fillHex: String, strokeHex: String, radiusPx: Int, strokePx: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            cornerRadius = radiusPx.toFloat()
            setStroke(strokePx, Color.parseColor(strokeHex))
        }

    fun dismiss(recordDismiss: Boolean = false) {
        val view = rootView ?: return
        val q = currentQuestion
        val elapsed = (System.currentTimeMillis() - shownAtMs).coerceAtLeast(0L)
        rootView = null
        currentQuestion = null
        mainHandler.removeCallbacksAndMessages(null)

        try {
            windowManager.removeViewImmediate(view)
        } catch (_: Exception) {
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {
            }
        }

        if (recordDismiss && q != null) {
            scope.launch(Dispatchers.IO) {
                repository.recordOutcome(q.word.id, ReviewOutcome.DISMISSED, elapsed)
            }
        }
    }

    fun destroy() {
        dismiss(recordDismiss = false)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "UnlockVocab"
    }
}
