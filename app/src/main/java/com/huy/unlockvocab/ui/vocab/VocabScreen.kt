package com.huy.unlockvocab.ui.vocab

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    var showAddDialog by remember { mutableStateOf(false) }

    fun refreshWords() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                repository.getStudyWords(searchQuery, posFilter, customOnly)
            }
            words = loaded
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
            Text(
                text = "Kho từ vựng (${words.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { showAddDialog = true }) {
                Text("+ Thêm từ")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
            listOf("noun", "verb", "adj", "adv").forEach { pos ->
                FilterChip(
                    selected = posFilter == pos,
                    onClick = { posFilter = if (posFilter == pos) "ALL" else pos },
                    label = { Text(pos) }
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
            onSpeakWord = onSpeakWord,
            onDismiss = { showAddDialog = false },
            onSaved = {
                showAddDialog = false
                refreshWords()
                onWordListChanged()
            }
        )
    }
}

@Composable
private fun WordItemCard(
    word: WordEntity,
    onSpeak: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${word.word} (${word.pos})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (word.cefr.isNotBlank()) {
                        Text(
                            text = " • ${word.cefr}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (word.isCustom) {
                        Text(
                            text = " • Tự thêm",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF047857)
                        )
                    }
                }
                Row {
                    TextButton(onClick = onSpeak) { Text("Nghe") }
                    if (word.isCustom) {
                        TextButton(onClick = onDelete) { Text("Xóa", color = Color(0xFFB91C1C)) }
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
    onSpeakWord: (String) -> Unit,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var wordInput by remember { mutableStateOf("") }
    var selectedPos by remember { mutableStateOf(PosTag.NOUN.code) }
    var ipaInput by remember { mutableStateOf("") }
    var cefrInput by remember { mutableStateOf("") }
    var primaryMeaning by remember { mutableStateOf("") }
    val extraMeanings = remember { mutableStateListOf<String>() }
    var exampleInput by remember { mutableStateOf("") }
    var isFromDictionary by remember { mutableStateOf(false) }

    var suggestions by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(wordInput) {
        val q = wordInput.trim()
        if (q.isEmpty()) {
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
        title = { Text("Thêm từ vào bộ học", fontWeight = FontWeight.Bold) },
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
                        onClick = { if (wordInput.isNotBlank()) onSpeakWord(wordInput.trim()) }
                    ) {
                        Text("Nghe")
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
                        TextButton(onClick = { extraMeanings.removeAt(idx) }) {
                            Text("Xóa")
                        }
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
                            topic = if (isFromDictionary) "OfflineDict" else "Custom",
                            primaryMeaning = cleanPrimary,
                            extraMeanings = cleanedExtras,
                            example = exampleInput,
                            isCustom = true
                        )
                        withContext(Dispatchers.Main) {
                            onSaved()
                        }
                    }
                }
            ) {
                Text("Lưu từ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
