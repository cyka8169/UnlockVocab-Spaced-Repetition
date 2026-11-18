package com.huy.unlockvocab.ui.stats

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.db.ReviewLogEntity
import com.huy.unlockvocab.data.db.ReviewOutcome
import com.huy.unlockvocab.data.db.StatsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Thống kê & Số liệu KHKT",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Tỷ lệ nhớ", "${stats.retentionPercent}%", Modifier.weight(1f))
            StatCard("Phản hồi TB", "${stats.avgResponseMs} ms", Modifier.weight(1f))
            StatCard("Thoát nhanh", "${stats.dismissedReviews}", Modifier.weight(1f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("Đã học", "${stats.studiedWords}/${stats.totalWords}", Modifier.weight(1f))
            StatCard("Trả lời đúng", "${stats.correctReviews}", Modifier.weight(1f))
            StatCard("Trả lời sai", "${stats.wrongReviews}", Modifier.weight(1f))
        }

        Button(
            onClick = {
                scope.launch {
                    val file = withContext(Dispatchers.IO) { repository.exportLogsToCsv() }
                    exportStatus = "Đã xuất: ${file.absolutePath}"
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
                        context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ số liệu CSV"))
                    } catch (_: Exception) {
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Xuất lịch sử ôn tập (CSV) cho báo cáo")
        }

        if (exportStatus != null) {
            Text(
                text = exportStatus!!,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF15803D)
            )
        }

        Text(
            text = "Lịch sử gần nhất",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        if (logs.isEmpty()) {
            Text(
                text = "Chưa có lượt ôn nào. Hãy thử mở khóa máy hoặc bấm Test Overlay ở Trang chủ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val sdf = remember { SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
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
                            Text(label, color = color, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
