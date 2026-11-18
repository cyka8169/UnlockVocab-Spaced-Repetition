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
import androidx.compose.material3.ButtonDefaults
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
import com.huy.unlockvocab.data.db.CefrLevel
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
    var cefrFilter by remember { mutableStateOf("ALL") }
    var customOnly by remember { mutableStateOf(false) }
    var words by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<WordEntity?>(null) }
    var showLevelImportDialog by remember { mutableStateOf(false) }
    var levelDialogRemoveMode by remember { mutableStateOf(false) }
    var showBulkDialog by remember { mutableStateOf(false) }
    var actionNotice by remember { mutableStateOf<String?>(null) }

    fun refreshWords() {
        scope.launch {
            val (loaded, total) = withContext(Dispatchers.IO) {
                repository.getStudyWords(searchQuery, posFilter, cefrFilter, customOnly) to repository.getTotalStudyWordCount()
            }
            words = loaded
            totalCount = total
        }
    }

    LaunchedEffect(searchQuery, posFilter, cefrFilter, customOnly) {
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
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = {
                    levelDialogRemoveMode = false
                    showLevelImportDialog = true
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+ Nạp theo cấp", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = {
                    levelDialogRemoveMode = true
                    showLevelImportDialog = true
                },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("- Xoá bớt từ", color = Color(0xFFB91C1C), maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = { showBulkDialog = true },
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier.weight(0.95f)
            ) {
                Text("+ Nhập nhanh", maxLines = 1, softWrap = false, style = MaterialTheme.typography.labelLarge)
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
                selected = posFilter == "ALL" && cefrFilter == "ALL" && !customOnly,
                onClick = {
                    posFilter = "ALL"
                    cefrFilter = "ALL"
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
            CefrLevel.codes.forEach { cefr ->
                FilterChip(
                    selected = cefrFilter == cefr,
                    onClick = { cefrFilter = if (cefrFilter == cefr) "ALL" else cefr },
                    label = { Text(cefr, maxLines = 1, softWrap = false) }
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

    if (showLevelImportDialog) {
        ImportByLevelDialog(
            repository = repository,
            defaultLevel = repository.settingsStore.load().userLevel,
            initialRemoveMode = levelDialogRemoveMode,
            onDismiss = { showLevelImportDialog = false },
            onImported = { added, chosenLevels ->
                showLevelImportDialog = false
                val levelStr = chosenLevels.joinToString(", ")
                actionNotice = if (added > 0) {
                    "Đã nạp thêm $added từ cấp độ [$levelStr] từ kho 50.000 mục offline."
                } else {
                    "Các từ thuộc bộ lọc [$levelStr] đã có đủ trong kho học của bạn."
                }
                refreshWords()
                onWordListChanged()
            },
            onRemoved = { removed, chosenLevels ->
                showLevelImportDialog = false
                val levelStr = chosenLevels.joinToString(", ")
                actionNotice = if (removed > 0) {
                    "Đã xoá bớt $removed từ cấp độ [$levelStr] (giữ nguyên 100% từ tự thêm)."
                } else {
                    "Không còn từ mặc định nào thuộc cấp [$levelStr] để xoá (từ tự thêm được giữ nguyên)."
                }
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
                                text = "Gợi ý từ điển 50.000 mục offline (chạm để điền nhanh):",
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

                // Optional CEFR Level selection
                Text(
                    text = "Cấp độ CEFR (tùy chọn):",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = cefrInput.isBlank(),
                        onClick = { cefrInput = "" },
                        label = { Text("Không đặt") }
                    )
                    CefrLevel.codes.forEach { lvl ->
                        FilterChip(
                            selected = cefrInput.equals(lvl, ignoreCase = true),
                            onClick = { cefrInput = lvl },
                            label = { Text(lvl) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImportByLevelDialog(
    repository: VocabRepository,
    defaultLevel: String,
    initialRemoveMode: Boolean = false,
    onDismiss: () -> Unit,
    onImported: (Int, List<String>) -> Unit,
    onRemoved: (Int, List<String>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var isRemoveMode by remember(initialRemoveMode) { mutableStateOf(initialRemoveMode) }
    val selectedLevels = remember(initialRemoveMode) {
        mutableStateListOf<String>().apply {
            val init = if (initialRemoveMode) {
                "A1"
            } else if (defaultLevel in CefrLevel.codes) {
                defaultLevel
            } else {
                "B1"
            }
            add(init)
        }
    }
    var selectedPos by remember { mutableStateOf("ALL") }
    var batchLimit by remember { mutableIntStateOf(50) }
    var levelCounts by remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    var removableCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val (counts, removable) = withContext(Dispatchers.IO) {
            repository.getCefrLevelCounts() to repository.getRemovableCefrCounts()
        }
        levelCounts = counts
        removableCounts = removable
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isRemoveMode) "Xoá bớt từ theo cấp độ (A1 – C2)" else "Nạp từ theo cấp độ (A1 – C2)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isRemoveMode,
                        onClick = {
                            isRemoveMode = false
                            if (batchLimit > 200) batchLimit = 50
                            errorMsg = null
                        },
                        label = { Text("+ Nạp thêm từ", maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = isRemoveMode,
                        onClick = {
                            isRemoveMode = true
                            errorMsg = null
                        },
                        label = { Text("- Xoá bớt từ", maxLines = 1, softWrap = false) }
                    )
                }

                Text(
                    text = if (isRemoveMode) {
                        "Xoá bớt từ có sẵn trong kho theo cấp độ bạn chọn (ưu tiên xoá từ chưa học trước). Toàn bộ từ Tự thêm được giữ nguyên 100%."
                    } else {
                        "Chọn 1 hoặc nhiều cấp độ CEFR bạn muốn thêm vào kho học (từ kho 50.000 mục từ điển offline):"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRemoveMode) Color(0xFF991B1B) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cấp độ CEFR:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(
                        onClick = {
                            if (selectedLevels.size == CefrLevel.codes.size) {
                                selectedLevels.clear()
                                selectedLevels.add(if (isRemoveMode) "A1" else defaultLevel.ifBlank { "B1" })
                            } else {
                                selectedLevels.clear()
                                selectedLevels.addAll(CefrLevel.codes)
                            }
                            errorMsg = null
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (selectedLevels.size == CefrLevel.codes.size) "Bỏ chọn bớt" else "Chọn cả A1–C2",
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CefrLevel.entries.forEach { level ->
                        val isSelected = level.code in selectedLevels
                        val badge = if (isRemoveMode) {
                            val rem = removableCounts[level.code]
                            if (rem != null) "${level.code} ($rem)" else level.code
                        } else {
                            val counts = levelCounts[level.code]
                            if (counts != null) "${level.code} (${counts.first}/${counts.second})" else level.code
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    selectedLevels.remove(level.code)
                                } else {
                                    selectedLevels.add(level.code)
                                }
                                errorMsg = null
                            },
                            label = { Text(badge, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Text(
                    text = if (isRemoveMode) "Lọc theo từ loại cần xoá:" else "Lọc theo từ loại cần nạp:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val posOptions = listOf(
                        "ALL" to "Tất cả",
                        "noun" to "noun",
                        "verb" to "verb",
                        "adj" to "adj",
                        "adv" to "adv",
                        "phrase" to "phrase"
                    )
                    posOptions.forEach { (code, label) ->
                        FilterChip(
                            selected = selectedPos == code,
                            onClick = { selectedPos = code },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Text(
                    text = if (isRemoveMode) "Số lượng từ muốn xoá bớt:" else "Số lượng từ nạp mỗi lần:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val countOptions = if (isRemoveMode) {
                        listOf(25 to "25 từ", 50 to "50 từ", 100 to "100 từ", 200 to "200 từ", 100_000 to "Xoá hết cấp này")
                    } else {
                        listOf(25 to "25 từ", 50 to "50 từ", 100 to "100 từ", 200 to "200 từ")
                    }
                    countOptions.forEach { (count, label) ->
                        FilterChip(
                            selected = batchLimit == count,
                            onClick = { batchLimit = count },
                            label = { Text(label, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                colors = if (isRemoveMode) {
                    ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C))
                } else {
                    ButtonDefaults.buttonColors()
                },
                onClick = {
                    if (selectedLevels.isEmpty()) {
                        errorMsg = "Hãy chọn ít nhất 1 cấp độ từ A1 đến C2."
                        return@Button
                    }
                    val chosen = CefrLevel.codes.filter { it in selectedLevels }
                    scope.launch {
                        if (isRemoveMode) {
                            val removed = withContext(Dispatchers.IO) {
                                repository.removeWordsByLevels(chosen, selectedPos, batchLimit)
                            }
                            onRemoved(removed, chosen)
                        } else {
                            val added = withContext(Dispatchers.IO) {
                                repository.importWordsByLevels(chosen, selectedPos, batchLimit)
                            }
                            onImported(added, chosen)
                        }
                    }
                }
            ) {
                Text(
                    text = if (isRemoveMode) "Xoá bớt từ" else "Nạp vào kho học",
                    maxLines = 1,
                    softWrap = false
                )
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
