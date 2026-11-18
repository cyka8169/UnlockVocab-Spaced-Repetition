package com.huy.unlockvocab.ui.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.db.CefrLevel
import com.huy.unlockvocab.data.db.DictionaryEntry
import com.huy.unlockvocab.data.db.PosTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DictionaryScreen(
    repository: VocabRepository,
    onSpeakWord: (String) -> Unit,
    onAddedToStudy: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var posFilter by remember { mutableStateOf("ALL") }
    var cefrFilter by remember { mutableStateOf("ALL") }
    var totalDictEntries by remember { mutableIntStateOf(50000) }
    var entries by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }
    var studyKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedEntry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var actionNotice by remember { mutableStateOf<String?>(null) }

    fun refreshResults() {
        scope.launch {
            val (total, loaded, keys) = withContext(Dispatchers.IO) {
                Triple(
                    repository.getTotalDictEntryCount(),
                    repository.lookupDictionary(query, posFilter, cefrFilter, limit = 80),
                    repository.getStudyWordKeySet()
                )
            }
            totalDictEntries = total
            entries = loaded
            studyKeys = keys
        }
    }

    LaunchedEffect(query, posFilter, cefrFilter) {
        refreshResults()
    }

    fun addEntry(entry: DictionaryEntry) {
        scope.launch {
            withContext(Dispatchers.IO) {
                repository.addDictEntryToStudy(entry)
            }
            studyKeys = studyKeys + "${entry.word}|${PosTag.fromCode(entry.pos).code}"
            actionNotice = "Đã thêm '${entry.word}' (${entry.pos}) vào kho học mở khóa."
            onAddedToStudy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Từ điển Offline ($totalDictEntries mục)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tra cứu Anh ↔ Việt • Phiên âm IPA • Phát âm & Thêm vào kho học 1 chạm",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Tra từ tiếng Anh, cụm từ hoặc nghĩa tiếng Việt...") },
            singleLine = true,
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(
                        onClick = { query = "" },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Xoá", maxLines = 1, softWrap = false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = posFilter == "ALL" && cefrFilter == "ALL",
                onClick = {
                    posFilter = "ALL"
                    cefrFilter = "ALL"
                },
                label = { Text("Tất cả", maxLines = 1, softWrap = false) }
            )
            listOf("noun", "verb", "adj", "adv", "phrase").forEach { pos ->
                FilterChip(
                    selected = posFilter == pos,
                    onClick = { posFilter = if (posFilter == pos) "ALL" else pos },
                    label = { Text(pos, maxLines = 1, softWrap = false) }
                )
            }
            CefrLevel.codes.forEach { cefr ->
                FilterChip(
                    selected = cefrFilter == cefr,
                    onClick = { cefrFilter = if (cefrFilter == cefr) "ALL" else cefr },
                    label = { Text(cefr, maxLines = 1, softWrap = false) }
                )
            }
        }

        if (actionNotice != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = actionNotice!!,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF15803D),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (query.isBlank()) {
                "Hiển thị ${entries.size} mục tiêu biểu (gõ từ bất kỳ để tra trong $totalDictEntries mục):"
            } else {
                "Tìm thấy ${entries.size} kết quả cho \"$query\":"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (entries.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Không tìm thấy mục từ phù hợp trong từ điển offline. Bạn có thể tự thêm từ mới ở tab Từ vựng → + Thêm từ.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    val normalizedKey = "${entry.word}|${PosTag.fromCode(entry.pos).code}"
                    val inStudy = normalizedKey in studyKeys
                    DictionaryEntryCard(
                        entry = entry,
                        inStudy = inStudy,
                        onOpenDetail = { selectedEntry = entry },
                        onSpeak = { onSpeakWord(entry.word) },
                        onAddToStudy = { addEntry(entry) }
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        val normalizedKey = "${entry.word}|${PosTag.fromCode(entry.pos).code}"
        DictionaryDetailDialog(
            entry = entry,
            inStudy = normalizedKey in studyKeys,
            repository = repository,
            onSpeak = { onSpeakWord(it) },
            onAddToStudy = { addEntry(entry) },
            onSelectRelated = { related -> selectedEntry = related },
            onDismiss = { selectedEntry = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryEntryCard(
    entry: DictionaryEntry,
    inStudy: Boolean,
    onOpenDetail: () -> Unit,
    onSpeak: () -> Unit,
    onAddToStudy: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = "${entry.word} (${entry.pos})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.cefr.isNotBlank() || entry.ipa.isNotBlank()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (entry.cefr.isNotBlank()) {
                                Text(
                                    text = entry.cefr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (entry.ipa.isNotBlank()) {
                                Text(
                                    text = entry.ipa,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TextButton(
                        onClick = onSpeak,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
                    ) {
                        Text("Nghe", color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
                    }
                    if (inStudy) {
                        Text(
                            text = "Đã có",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    } else {
                        TextButton(
                            onClick = onAddToStudy,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
                        ) {
                            Text("+ Học", color = Color(0xFF047857), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            if (entry.meanings.isNotEmpty()) {
                Text(
                    text = entry.meanings.joinToString("; "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (entry.example.isNotBlank()) {
                Text(
                    text = "VD: ${entry.example}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DictionaryDetailDialog(
    entry: DictionaryEntry,
    inStudy: Boolean,
    repository: VocabRepository,
    onSpeak: (String) -> Unit,
    onAddToStudy: () -> Unit,
    onSelectRelated: (DictionaryEntry) -> Unit,
    onDismiss: () -> Unit
) {
    var related by remember(entry.id) { mutableStateOf<List<DictionaryEntry>>(emptyList()) }

    LaunchedEffect(entry.id) {
        related = withContext(Dispatchers.IO) {
            repository.getRelatedDictEntries(entry.word, entry.id, limit = 8)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "${entry.word} (${entry.pos})",
                    fontWeight = FontWeight.Bold
                )
                val meta = listOfNotNull(
                    entry.cefr.takeIf { it.isNotBlank() }?.let { "CEFR $it" },
                    entry.ipa.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSpeak(entry.word) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Nghe phát âm tiếng Anh", maxLines = 1, softWrap = false)
                }

                Text(
                    text = "Nghĩa tiếng Việt:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                entry.meanings.forEachIndexed { idx, meaning ->
                    Text(
                        text = "${idx + 1}. $meaning",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (entry.example.isNotBlank()) {
                    HorizontalDivider()
                    Text(
                        text = "Câu ví dụ:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = entry.example,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (related.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Cụm từ & Từ liên quan:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        related.forEach { rel ->
                            FilterChip(
                                selected = false,
                                onClick = { onSelectRelated(rel) },
                                label = { Text("${rel.word} (${rel.pos})", maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (inStudy) {
                Button(onClick = onDismiss) {
                    Text("Đã có trong kho học", maxLines = 1, softWrap = false)
                }
            } else {
                Button(onClick = onAddToStudy) {
                    Text("+ Đưa vào kho học", maxLines = 1, softWrap = false)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
