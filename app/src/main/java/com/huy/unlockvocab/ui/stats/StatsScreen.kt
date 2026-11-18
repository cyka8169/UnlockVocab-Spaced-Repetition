package com.huy.unlockvocab.ui.stats

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.huy.unlockvocab.data.db.DailyStatPoint
import com.huy.unlockvocab.data.db.ReviewLogEntity
import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.StatsSummary
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
    var exportStatus by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(stats.totalReviews) {
        logs = withContext(Dispatchers.IO) { repository.getRecentLogs() }
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Thuộc vững: ${stats.masteredWords}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), maxLines = 1, softWrap = false)
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
