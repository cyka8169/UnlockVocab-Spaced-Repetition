package com.huy.unlockvocab.ui.vocab

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
import androidx.compose.runtime.mutableStateListOf
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
import com.huy.unlockvocab.data.db.DictionaryEntry
import com.huy.unlockvocab.data.db.PosTag
import com.huy.unlockvocab.data.db.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VocabScreen(
    repository: VocabRepository,
    onSpeakWord: (String) -> Unit,
    onWordListChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var posFilter by remember { mutableStateOf("ALL") }
    var customOnly by remember { mutableStateOf(false) }
    var words by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<WordEntity?>(null) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var actionNotice by remember { mutableStateOf<String?>(null) }

    fun refreshWords() {
        scope.launch {
            val (loaded, total) = withContext(Dispatchers.IO) {
                repository.getStudyWords(searchQuery, posFilter, customOnly) to repository.getTotalStudyWordCount()
            }
            words = loaded
            totalCount = total
        }
    }

    LaunchedEffect(searchQuery, posFilter, customOnly) {
        refreshWords()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val countText = if (totalCount == 0 || words.size == totalCount) {
                "${totalCount.coerceAtLeast(words.size)} từ"
            } else {
                "${words.size}/$totalCount từ"
            }
            Text(
                text = "Kho từ vựng ($countText)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Button(
                onClick = {
                    editingWord = null
                    showAddDialog = true
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("+ Thêm từ", maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val added = withContext(Dispatchers.IO) {
                            repository.importTopicPack("IELTS_B1_B2")
                        }
                        actionNotice = if (added > 0) {
                            "Đã nạp thêm $added từ B1–B2 (IELTS/THPT) từ kho từ điển offline."
                        } else {
                            "Gói từ B1–B2 đã có đủ trong kho học của bạn."
                        }
                        refreshWords()
                        onWordListChanged()
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Nạp gói B1–B2", maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = { showBulkDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Nhập nhanh", maxLines = 1, softWrap = false)
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

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Tìm từ hoặc nghĩa tiếng Việt...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = posFilter == "ALL" && !customOnly,
                onClick = {
                    posFilter = "ALL"
                    customOnly = false
                },
                label = { Text("Tất cả") }
            )
            FilterChip(
                selected = customOnly,
                onClick = { customOnly = !customOnly },
                label = { Text("Tự thêm") }
            )
            listOf("noun", "verb", "adj", "adv", "phrase").forEach { pos ->
                FilterChip(
                    selected = posFilter == pos,
                    onClick = { posFilter = if (posFilter == pos) "ALL" else pos },
                    label = { Text(pos, maxLines = 1, softWrap = false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(words, key = { it.id }) { item ->
                WordItemCard(
                    word = item,
                    onSpeak = { onSpeakWord(item.word) },
                    onEdit = {
                        editingWord = item
                        showAddDialog = true
                    },
                    onDelete = {
                        scope.launch(Dispatchers.IO) {
                            repository.deleteWord(item.id)
                            withContext(Dispatchers.Main) {
                                refreshWords()
                                onWordListChanged()
                            }
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddWordDialog(
            repository = repository,
            initialWord = editingWord,
            onSpeakWord = onSpeakWord,
            onDismiss = {
                showAddDialog = false
                editingWord = null
            },
            onSaved = {
                showAddDialog = false
                editingWord = null
                refreshWords()
                onWordListChanged()
            }
        )
    }

    if (showBulkDialog) {
        BulkImportDialog(
            onDismiss = { showBulkDialog = false },
            onImport = { rawText ->
                scope.launch {
                    val count = withContext(Dispatchers.IO) {
                        repository.importBulkCsvLines(rawText)
                    }
                    showBulkDialog = false
                    actionNotice = "Đã nhập thêm $count từ mới từ danh sách nhanh."
                    refreshWords()
                    onWordListChanged()
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordItemCard(
    word: WordEntity,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                        text = "${word.word} (${word.pos})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (word.cefr.isNotBlank() || word.isCustom) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (word.cefr.isNotBlank()) {
                                Text(
                                    text = word.cefr,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (word.isCustom) {
                                Text(
                                    text = "Tự thêm",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CompactTextAction("Nghe", MaterialTheme.colorScheme.primary, onSpeak)
                    CompactTextAction("Sửa", MaterialTheme.colorScheme.primary, onEdit)
                    if (word.isCustom) {
                        CompactTextAction("Xóa", Color(0xFFB91C1C), onDelete)
                    }
                }
            }

            if (word.ipa.isNotBlank()) {
                Text(
                    text = word.ipa,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Nghĩa chính: ${word.primaryMeaning}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (word.extraMeanings.isNotEmpty()) {
                Text(
                    text = "Nghĩa khác: ${word.extraMeanings.joinToString("; ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (word.example.isNotBlank()) {
                Text(
                    text = "VD: ${word.example}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "SM-2: lặp ${word.repetitions} | đúng ${word.totalCorrect} / sai ${word.totalWrong} | EF ${"%.2f".format(word.easeFactor)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog supporting:
 * 1) Live offline autocomplete suggestions from dict_entries (9,500+ words).
 * 2) Structured custom word creation enforcing Word + POS + Primary meaning + multiple extra meanings.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddWordDialog(
    repository: VocabRepository,
    initialWord: WordEntity? = null,
    onSpeakWord: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEditing = initialWord != null
    var wordInput by remember(initialWord) { mutableStateOf(initialWord?.word.orEmpty()) }
    var selectedPos by remember(initialWord) { mutableStateOf(initialWord?.pos ?: PosTag.NOUN.code) }
    var ipaInput by remember(initialWord) { mutableStateOf(initialWord?.ipa.orEmpty()) }
    var cefrInput by remember(initialWord) { mutableStateOf(initialWord?.cefr.orEmpty()) }
    var primaryMeaning by remember(initialWord) { mutableStateOf(initialWord?.primaryMeaning.orEmpty()) }
    val extraMeanings = remember(initialWord) {
        mutableStateListOf<String>().apply {
            if (initialWord != null) addAll(initialWord.extraMeanings)
        }
    }
    var exampleInput by remember(initialWord) { mutableStateOf(initialWord?.example.orEmpty()) }
    var isFromDictionary by remember(initialWord) { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(wordInput, isEditing) {
        val q = wordInput.trim()
        if (isEditing || q.isEmpty()) {
            suggestions = emptyList()
        } else {
            val found = withContext(Dispatchers.IO) {
                repository.searchDictionary(q)
            }
            suggestions = found
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Chỉnh sửa từ vựng" else "Thêm từ vào bộ học",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = wordInput,
                        onValueChange = {
                            wordInput = it
                            isFromDictionary = false
                            validationError = null
                        },
                        label = { Text("Từ vựng (bắt buộc) *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = { if (wordInput.isNotBlank()) onSpeakWord(wordInput.trim()) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.defaultMinSize(minWidth = 1.dp)
                    ) {
                        Text("Nghe", maxLines = 1, softWrap = false)
                    }
                }

                // Live offline dictionary autocomplete suggestions
                if (suggestions.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Gợi ý từ điển offline (chạm để điền nhanh):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            suggestions.take(5).forEachIndexed { index, entry ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            wordInput = entry.word
                                            selectedPos = PosTag.fromCode(entry.pos).code
                                            ipaInput = entry.ipa
                                            cefrInput = entry.cefr
                                            primaryMeaning = entry.meanings.firstOrNull().orEmpty()
                                            extraMeanings.clear()
                                            extraMeanings.addAll(entry.meanings.drop(1))
                                            exampleInput = entry.example
                                            isFromDictionary = true
                                            validationError = null
                                        }
                                        .padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${entry.word} (${entry.pos}) ${entry.ipa}",
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = entry.meanings.joinToString("; "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (index < suggestions.take(5).lastIndex) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                } else if (wordInput.trim().length >= 2) {
                    Text(
                        text = "Không có sẵn trong từ điển offline — bạn có thể tự nhập đầy đủ Từ loại và Các nghĩa bên dưới.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB45309)
                    )
                }

                // Required POS selection
                Text(
                    text = "Từ loại (bắt buộc để sinh đáp án nhiễu) *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosTag.entries.forEach { tag ->
                        FilterChip(
                            selected = selectedPos == tag.code,
                            onClick = { selectedPos = tag.code },
                            label = { Text("${tag.code} (${tag.viLabel})") }
                        )
                    }
                }

                // Required primary meaning
                OutlinedTextField(
                    value = primaryMeaning,
                    onValueChange = {
                        primaryMeaning = it
                        validationError = null
                    },
                    label = { Text("Nghĩa chính (hiện trong trắc nghiệm) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Multiple extra meanings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Các nghĩa khác (tùy chọn):", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { extraMeanings.add("") }) {
                        Text("+ Thêm nghĩa")
                    }
                }
                extraMeanings.forEachIndexed { idx, value ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = value,
                            onValueChange = { extraMeanings[idx] = it },
                            label = { Text("Nghĩa phụ ${idx + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        CompactTextAction("Xóa", Color(0xFFB91C1C)) { extraMeanings.removeAt(idx) }
                    }
                }

                OutlinedTextField(
                    value = ipaInput,
                    onValueChange = { ipaInput = it },
                    label = { Text("Phiên âm IPA (tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exampleInput,
                    onValueChange = { exampleInput = it },
                    label = { Text("Câu ví dụ (tùy chọn)") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Color(0xFFB91C1C),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanWord = wordInput.trim()
                    val cleanPrimary = primaryMeaning.trim()
                    if (cleanWord.isEmpty()) {
                        validationError = "Vui lòng nhập từ vựng tiếng Anh."
                        return@Button
                    }
                    if (selectedPos.isBlank()) {
                        validationError = "Vui lòng chọn từ loại."
                        return@Button
                    }
                    if (cleanPrimary.isEmpty()) {
                        validationError = "Vui lòng nhập ít nhất 1 nghĩa chính tiếng Việt."
                        return@Button
                    }

                    val cleanedExtras = extraMeanings.map { it.trim() }.filter { it.isNotEmpty() }
                    scope.launch(Dispatchers.IO) {
                        repository.addOrUpdateCustomWord(
                            word = cleanWord,
                            pos = selectedPos,
                            ipa = ipaInput,
                            cefr = cefrInput,
                            topic = initialWord?.topic ?: if (isFromDictionary) "OfflineDict" else "Custom",
                            primaryMeaning = cleanPrimary,
                            extraMeanings = cleanedExtras,
                            example = exampleInput,
                            isCustom = initialWord?.isCustom ?: true,
                            editingWordId = initialWord?.id
                        )
                        withContext(Dispatchers.Main) {
                            onSaved()
                        }
                    }
                }
            ) {
                Text(if (isEditing) "Cập nhật" else "Lưu từ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun CompactTextAction(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 32.dp)
    ) {
        Text(label, color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun BulkImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var bulkText by remember {
        mutableStateOf("algorithm, noun, thuật toán; giải thuật\npersevere, verb, kiên trì; bền bỉ")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nhập nhanh nhiều từ", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mỗi dòng 1 từ theo mẫu:\ntừ, từ_loại (noun/verb/adj/adv), nghĩa_chính; nghĩa_phụ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = bulkText,
                    onValueChange = { bulkText = it },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onImport(bulkText) }) {
                Text("Nhập danh sách")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
