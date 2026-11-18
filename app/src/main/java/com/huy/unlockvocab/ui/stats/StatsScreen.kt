package com.huy.unlockvocab.ui.stats

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.db.CefrLevel
import com.huy.unlockvocab.data.db.DailyStatPoint
import com.huy.unlockvocab.data.db.KhktTestRecord
import com.huy.unlockvocab.data.db.QuizQuestion
import com.huy.unlockvocab.data.db.RetentionMilestone
import com.huy.unlockvocab.data.db.ReviewLogEntity
import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.StatsSummary
import com.huy.unlockvocab.domain.CoreSelfTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    repository: VocabRepository,
    stats: StatsSummary
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<ReviewLogEntity>>(emptyList()) }
    var testRecords by remember { mutableStateOf<List<KhktTestRecord>>(emptyList()) }
    var activeAssessmentType by remember { mutableStateOf<String?>(null) }
    var selfTestSummary by remember { mutableStateOf<Pair<Boolean, List<String>>?>(null) }
    var exportStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stats.totalReviews) {
        val (loadedLogs, loadedTests) = withContext(Dispatchers.IO) {
            repository.getRecentLogs() to repository.getKhktTestRecords()
        }
        logs = loadedLogs
        testRecords = loadedTests
    }

    val sdf = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Thống kê & Số liệu KHKT",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Tỷ lệ nhớ", "${stats.retentionPercent}%", Modifier.weight(1f))
                StatCard("Phản hồi TB", "${stats.avgResponseMs} ms", Modifier.weight(1f))
                StatCard("Thoát nhanh", "${stats.dismissedReviews}", Modifier.weight(1f))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard("Đã học", "${stats.studiedWords}/${stats.totalWords}", Modifier.weight(1f))
                StatCard("Trả lời đúng", "${stats.correctReviews}", Modifier.weight(1f))
                StatCard("Trả lời sai", "${stats.wrongReviews}", Modifier.weight(1f))
            }
        }

        // Mastery breakdown bar (SM-2)
        item {
            MasteryBreakdownCard(stats)
        }

        // Ebbinghaus Forgetting Curve vs Actual SM-2 Retention (D1 / D3 / D7 / D21)
        if (stats.retentionMilestones.isNotEmpty()) {
            item {
                EbbinghausComparisonCard(stats.retentionMilestones)
            }
        }

        // KHKT Pre-test / Post-test (15 questions) & Core Self-Test runner
        item {
            KhktPrePostTestCard(
                records = testRecords,
                selfTestSummary = selfTestSummary,
                onStartPreTest = { activeAssessmentType = "PRE_TEST" },
                onStartPostTest = { activeAssessmentType = "POST_TEST" },
                onExportTestsCsv = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { repository.exportKhktTestsToCsv() }
                        exportStatus = "Đã xuất KHKT Test CSV: ${file.absolutePath}"
                        shareCsvFile(context, file, "Chia sẻ dữ liệu Pre/Post-test KHKT")
                    }
                },
                onRunSelfTest = {
                    scope.launch {
                        val res = withContext(Dispatchers.IO) { CoreSelfTester.runAll(repository) }
                        selfTestSummary = res
                    }
                }
            )
        }

        // 7-day bar chart
        if (stats.last7Days.isNotEmpty()) {
            item {
                Last7DaysChartCard(stats.last7Days)
            }
        }

        // Top hardest words
        if (stats.hardestWords.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Top từ cần ôn kỹ (hay sai nhất)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        stats.hardestWords.forEach { w ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${w.word} (${w.pos}) — ${w.primaryMeaning}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Text(
                                    text = "Sai ${w.totalWrong} • EF ${"%.2f".format(w.easeFactor)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }

        // CSV Export buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) { repository.exportLogsToCsv() }
                            exportStatus = "Đã xuất log: ${file.absolutePath}"
                            shareCsvFile(context, file, "Chia sẻ lịch sử ôn tập CSV")
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Xuất log CSV", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val file = withContext(Dispatchers.IO) { repository.exportWordsToCsv() }
                            exportStatus = "Đã xuất kho từ: ${file.absolutePath}"
                            shareCsvFile(context, file, "Chia sẻ kho từ vựng CSV")
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Xuất kho từ CSV", maxLines = 1, softWrap = false)
                }
            }
        }

        if (exportStatus != null) {
            item {
                Text(
                    text = exportStatus!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF15803D)
                )
            }
        }

        item {
            Text(
                text = "Lịch sử gần nhất",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = "Chưa có lượt ôn nào. Hãy thử mở khóa máy hoặc bấm Test Overlay ở Trang chủ.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs, key = { it.id }) { log ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("${log.word} (${log.pos})", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${sdf.format(Date(log.timestamp))} • ${log.responseTimeMs} ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val (label, color) = when (log.outcome) {
                            ReviewOutcome.CORRECT -> "Đúng" to Color(0xFF15803D)
                            ReviewOutcome.WRONG -> "Sai" to Color(0xFFB91C1C)
                            ReviewOutcome.DISMISSED -> "Thoát nhanh" to Color(0xFF64748B)
                        }
                        Text(label, color = color, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    activeAssessmentType?.let { testType ->
        KhktAssessmentDialog(
            repository = repository,
            testType = testType,
            onDismiss = { activeAssessmentType = null },
            onFinished = { saved ->
                activeAssessmentType = null
                testRecords = listOf(saved) + testRecords
                exportStatus = "Đã lưu kết quả ${if (saved.testType == "PRE_TEST") "Pre-test" else "Post-test"}: ${saved.score}/${saved.totalQuestions} (${saved.accuracyPercent}% • ${saved.avgResponseMs} ms)."
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MasteryBreakdownCard(stats: StatsSummary) {
    val total = stats.totalWords.coerceAtLeast(1)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Phân bố mức độ ghi nhớ (SM-2)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFFE2E8F0))
            ) {
                if (stats.masteredWords > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(stats.masteredWords.toFloat() / total)
                            .background(Color(0xFF16A34A))
                    )
                }
                if (stats.consolidatingWords > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(stats.consolidatingWords.toFloat() / total)
                            .background(Color(0xFF2563EB))
                    )
                }
                if (stats.learningWords > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(stats.learningWords.toFloat() / total)
                            .background(Color(0xFFF59E0B))
                    )
                }
                if (stats.unseenWords > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(stats.unseenWords.toFloat() / total)
                            .background(Color(0xFFCBD5E1))
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Thuộc sâu (≥21d): ${stats.masteredWords}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), maxLines = 1, softWrap = false)
                Text("Mới nhớ (6–20d): ${stats.consolidatingWords}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1D4ED8), maxLines = 1, softWrap = false)
                Text("Đang học: ${stats.learningWords}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), maxLines = 1, softWrap = false)
                Text("Chưa học: ${stats.unseenWords}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569), maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun Last7DaysChartCard(days: List<DailyStatPoint>) {
    val maxDayTotal = days.maxOfOrNull { it.correct + it.wrong + it.dismissed }?.coerceAtLeast(1) ?: 1
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Biểu đồ thực nghiệm 7 ngày gần nhất",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { d ->
                    val dayTotal = d.correct + d.wrong + d.dismissed
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = if (dayTotal > 0) "$dayTotal" else "",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((72f * dayTotal / maxDayTotal).coerceAtLeast(if (dayTotal > 0) 6f else 2f).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    when {
                                        dayTotal == 0 -> Color(0xFFE2E8F0)
                                        d.correct >= d.wrong -> Color(0xFF2563EB)
                                        else -> Color(0xFFDC2626)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(d.dateLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text = "Xanh: chủ yếu đúng • Đỏ: cần ôn kỹ • Con số trên cột là tổng lượt mở khóa/ôn trong ngày.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun shareCsvFile(context: Context, file: File, chooserTitle: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    } catch (_: Exception) {
    }
}

@Composable
private fun EbbinghausComparisonCard(milestones: List<RetentionMilestone>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Đối chiếu Đường cong quên Ebbinghaus (D1–D21)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "So sánh tỷ lệ nhớ tự nhiên khi học dồn (Ebbinghaus) với kết quả ôn giãn cách SM-2 của bạn:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mốc", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                Text("Ebbinghaus", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("SM-2 thực tế", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
            }
            HorizontalDivider()
            milestones.forEach { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = m.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "~${m.ebbinghausBaselinePercent}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.weight(1f)
                    )
                    val sm2Text = if (m.wordCount > 0) {
                        "${m.actualRetentionPercent}% (${m.wordCount} từ)"
                    } else {
                        "Chưa tới mốc"
                    }
                    val sm2Color = if (m.wordCount > 0 && m.actualRetentionPercent >= m.ebbinghausBaselinePercent) {
                        Color(0xFF15803D)
                    } else {
                        Color(0xFF475569)
                    }
                    Text(
                        text = sm2Text,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = sm2Color,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KhktPrePostTestCard(
    records: List<KhktTestRecord>,
    selfTestSummary: Pair<Boolean, List<String>>?,
    onStartPreTest: () -> Unit,
    onStartPostTest: () -> Unit,
    onExportTestsCsv: () -> Unit,
    onRunSelfTest: () -> Unit
) {
    val latestPre = records.firstOrNull { it.testType == "PRE_TEST" }
    val latestPost = records.firstOrNull { it.testType == "POST_TEST" }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Đo lường đối chứng KHKT (Pre / Post-test 15 câu)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Pre-test",
                    value = latestPre?.let { "${it.accuracyPercent}%" } ?: "Chưa làm",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Post-test",
                    value = latestPost?.let { "${it.accuracyPercent}%" } ?: "Chưa làm",
                    modifier = Modifier.weight(1f)
                )
                val deltaText = if (latestPre != null && latestPost != null) {
                    val diff = latestPost.accuracyPercent - latestPre.accuracyPercent
                    if (diff >= 0) "+$diff%" else "$diff%"
                } else {
                    "—"
                }
                StatCard(
                    label = "Tăng trưởng",
                    value = deltaText,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = onStartPreTest,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Làm Pre-test", maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = onStartPostTest,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Làm Post-test", maxLines = 1, softWrap = false)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onExportTestsCsv,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Xuất KHKT Test CSV", maxLines = 1, softWrap = false)
                }
                OutlinedButton(
                    onClick = onRunSelfTest,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tự kiểm thử lõi", maxLines = 1, softWrap = false)
                }
            }

            if (selfTestSummary != null) {
                val (passed, lines) = selfTestSummary
                val passedCount = lines.count { it.startsWith("[PASS]") }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (passed) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, if (passed) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (passed) {
                                "SELF_TEST_PASS ($passedCount/${lines.size} kiểm tra lõi đạt)"
                            } else {
                                "SELF_TEST_FAIL ($passedCount/${lines.size} kiểm tra lõi đạt)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (passed) Color(0xFF15803D) else Color(0xFF991B1B)
                        )
                        lines.forEach { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (line.startsWith("[PASS]")) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KhktAssessmentDialog(
    repository: VocabRepository,
    testType: String,
    onDismiss: () -> Unit,
    onFinished: (KhktTestRecord) -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedLevel by remember { mutableStateOf("ALL") }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var totalElapsedMs by remember { mutableLongStateOf(0L) }
    var questionStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(selectedLevel) {
        val loaded = withContext(Dispatchers.IO) {
            repository.buildKhktAssessmentQuiz(levelFilter = selectedLevel, count = 15)
        }
        questions = loaded
        currentIndex = 0
        correctCount = 0
        totalElapsedMs = 0L
        questionStartedAt = System.currentTimeMillis()
    }

    val titleLabel = if (testType == "PRE_TEST") "Kiểm tra đầu vào (Pre-test)" else "Kiểm tra sau thực nghiệm (Post-test)"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleLabel, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentIndex == 0) {
                    Text("Chọn phạm vi cấp độ:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf("ALL") + CefrLevel.codes).forEach { lvl ->
                            FilterChip(
                                selected = selectedLevel == lvl,
                                onClick = { selectedLevel = lvl },
                                label = { Text(if (lvl == "ALL") "Tất cả" else lvl, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                val currentQ = questions.getOrNull(currentIndex)
                if (currentQ == null) {
                    Text("Đang chuẩn bị đề kiểm tra 15 câu...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = "Câu ${currentIndex + 1}/${questions.size} • Đúng hiện tại: $correctCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${currentQ.word.word} (${currentQ.word.pos})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (currentQ.word.ipa.isNotBlank()) {
                        Text(
                            text = currentQ.word.ipa,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val labels = listOf("A", "B", "C", "D")
                    currentQ.options.forEachIndexed { idx, opt ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    val nextTotalElapsed = totalElapsedMs + (now - questionStartedAt).coerceAtLeast(100L)
                                    val nextCorrect = correctCount + if (opt.isCorrect) 1 else 0
                                    if (currentIndex + 1 < questions.size) {
                                        totalElapsedMs = nextTotalElapsed
                                        correctCount = nextCorrect
                                        currentIndex += 1
                                        questionStartedAt = System.currentTimeMillis()
                                    } else {
                                        val avgMs = nextTotalElapsed / questions.size.coerceAtLeast(1)
                                        scope.launch {
                                            val saved = withContext(Dispatchers.IO) {
                                                repository.saveKhktTestResult(
                                                    testType = testType,
                                                    level = selectedLevel,
                                                    score = nextCorrect,
                                                    totalQuestions = questions.size,
                                                    avgResponseMs = avgMs
                                                )
                                            }
                                            onFinished(saved)
                                        }
                                    }
                                },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "${labels.getOrElse(idx) { "${idx + 1}" }}. ${opt.meaning}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy bài test")
            }
        }
    )
}
