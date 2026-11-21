package com.huy.unlockvocab.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.huy.unlockvocab.BuildConfig
import com.huy.unlockvocab.data.db.AppSettings
import com.huy.unlockvocab.data.db.CefrLevel
import com.huy.unlockvocab.data.db.PosTag
import com.huy.unlockvocab.data.db.QuizQuestionStyle
import com.huy.unlockvocab.data.db.StatsSummary
import com.huy.unlockvocab.data.db.TriggerMode
import com.huy.unlockvocab.data.db.WordSelectionPolicy
import com.huy.unlockvocab.security.IntegrityStatus
import com.huy.unlockvocab.security.OwnershipGuard
import com.huy.unlockvocab.service.DeviceKeepAliveHelper
import com.huy.unlockvocab.service.XiaomiKeepAliveStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    stats: StatsSummary,
    integrityStatus: IntegrityStatus,
    xiaomiStatus: XiaomiKeepAliveStatus,
    snoozeRemainingMinutes: Int,
    todayAnsweredCount: Int,
    filterMatchCount: Int,
    userLevelNotice: String?,
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    isIgnoringBatteryOpt: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit,
    onQuickImportCurrentLevel: (String) -> Unit,
    onSnooze30Minutes: () -> Unit,
    onResumeFromSnooze: () -> Unit,
    onTestOverlayNow: () -> Unit
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "UnlockVocab",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Học từ vựng Spaced Repetition ngay khi mở khóa màn hình • Offline 100%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!integrityStatus.isAuthentic) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cảnh báo: Phát hiện APK đã bị chỉnh sửa chữ ký hoặc đóng gói lại trái phép (${integrityStatus.reason}). Bản gốc thuộc về tác giả ${integrityStatus.authorDisplay} (com.huy.unlockvocab).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF991B1B),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Quick status summary
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricMini("Kho học", "${stats.totalWords} từ")
                MetricMini("Trình độ", settings.userLevel)
                MetricMini("Đến hạn", "${stats.dueWords} từ")
                MetricMini("Đã nhớ", "${stats.retentionPercent}%")
            }
        }

        // Permissions onboarding card (essential for Android 14+ / Samsung One UI / Xiaomi HyperOS)
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (xiaomiStatus.isXiaomiHyperOs) {
                        "Quyền chạy nền (${xiaomiStatus.osLabel})"
                    } else {
                        "Quyền chạy nền (Android / Samsung)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TriStatePermissionRow(
                    title = "Hiển thị trên ứng dụng khác (Overlay)",
                    granted = hasOverlayPermission,
                    actionLabel = "Cấp quyền",
                    onAction = { DeviceKeepAliveHelper.openOverlayPermissionSettings(context) }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    TriStatePermissionRow(
                        title = "Thông báo dịch vụ nền",
                        granted = hasNotificationPermission,
                        actionLabel = "Cấp quyền",
                        onAction = onRequestNotificationPermission
                    )
                }
                TriStatePermissionRow(
                    title = "Tắt tối ưu pin để không bị ngủ nền",
                    granted = isIgnoringBatteryOpt,
                    actionLabel = "Mở cài đặt",
                    onAction = { DeviceKeepAliveHelper.openBatterySettings(context) }
                )
                if (xiaomiStatus.isXiaomiHyperOs) {
                    TriStatePermissionRow(
                        title = "Tự khởi chạy nền (AutoStart)",
                        granted = xiaomiStatus.autoStartGranted,
                        actionLabel = "Mở cài đặt",
                        grantedText = "Đã bật",
                        deniedText = "Chưa bật — cần bật để tự chạy lại",
                        unknownText = "Cần kiểm tra thủ công",
                        onAction = { DeviceKeepAliveHelper.openAutoStartSettings(context) }
                    )
                    TriStatePermissionRow(
                        title = "Khóa ứng dụng trong Đa nhiệm (tránh nút X)",
                        granted = xiaomiStatus.lockedInRecents,
                        actionLabel = "Mở Bảo mật",
                        grantedText = "Đã khóa (an toàn khi bấm X)",
                        deniedText = "Chưa khóa — nút (X) sẽ đóng app nền",
                        unknownText = "Khuyến nghị bật",
                        onAction = { DeviceKeepAliveHelper.openRecentsLockSettings(context) }
                    )
                    if (xiaomiStatus.lockedInRecents != true || xiaomiStatus.autoStartGranted != true) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Mẹo Xiaomi/HyperOS: Mở màn hình Đa nhiệm → nhấn giữ thẻ UnlockVocab → chạm biểu tượng ổ khóa 🔒 (hoặc Bảo mật → Tăng tốc độ → ⚙️ → Khóa ứng dụng) và bật Tự khởi chạy để nhấn nút (X) không tắt câu hỏi mở khóa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Master Service switch + Instant test button
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bật câu hỏi khi mở khóa", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (settings.serviceEnabled) "Dịch vụ nền đang bật" else "Đang tạm tắt",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.serviceEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(settings.copy(serviceEnabled = enabled))
                        }
                    )
                }

                Button(
                    onClick = onTestOverlayNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test hiện câu hỏi Overlay ngay", maxLines = 1, softWrap = false)
                }
                if (snoozeRemainingMinutes > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        border = BorderStroke(1.dp, Color(0xFFFB923C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Đang tạm nghỉ (còn $snoozeRemainingMinutes phút)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF9A3412),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Mở khóa sẽ tạm không bật câu hỏi cho đến khi hết giờ.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF7C2D12)
                                )
                            }
                            OutlinedButton(
                                onClick = onResumeFromSnooze,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                            ) {
                                Text("Bật lại ngay", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                } else if (settings.serviceEnabled) {
                    OutlinedButton(
                        onClick = onSnooze30Minutes,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tạm nghỉ 30 phút", maxLines = 1, softWrap = false)
                    }
                }
                Text(
                    text = "Mẹo: Khi thẻ hiện lên, chạm vào bất kỳ chỗ nào ngoài 4 ô đáp án sẽ tắt câu hỏi ngay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // User's Proficiency Level (A1 - C2) & Quick Level Import
        val currentCefr = CefrLevel.fromCode(settings.userLevel)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Trình độ của bạn (A1 – C2)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CefrLevel.entries.forEach { level ->
                        FilterChip(
                            selected = settings.userLevel == level.code,
                            onClick = { onUpdateSettings(settings.copy(userLevel = level.code)) },
                            label = { Text(level.code, maxLines = 1, softWrap = false) }
                        )
                    }
                }
                Text(
                    text = "${currentCefr.code} • ${currentCefr.shortTitle}: ${currentCefr.description} (ưu tiên chọn từ mới ở trình độ này).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { onQuickImportCurrentLevel(currentCefr.code) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "+ Nạp thêm 50 từ cấp ${currentCefr.code} vào kho học",
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (!userLevelNotice.isNullOrBlank()) {
                    Text(
                        text = userLevelNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Trigger Policy (flexible: every unlock, every N unlocks, or cooldown minutes)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tần suất kích hoạt khi mở khóa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TriggerMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.triggerMode == mode,
                            onClick = { onUpdateSettings(settings.copy(triggerMode = mode)) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.label, fontWeight = FontWeight.Medium)
                            Text(
                                mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (settings.triggerMode == TriggerMode.EVERY_N_UNLOCKS) {
                    StepperRow(
                        label = "Số lần mở khóa mỗi lượt:",
                        valueText = "${settings.everyNUnlocks} lần",
                        onMinus = {
                            onUpdateSettings(settings.copy(everyNUnlocks = (settings.everyNUnlocks - 1).coerceAtLeast(1)))
                        },
                        onPlus = {
                            onUpdateSettings(settings.copy(everyNUnlocks = (settings.everyNUnlocks + 1).coerceAtMost(20)))
                        }
                    )
                }

                if (settings.triggerMode == TriggerMode.COOLDOWN_MINUTES) {
                    StepperRow(
                        label = "Khoảng nghỉ tối thiểu:",
                        valueText = "${settings.cooldownMinutes} phút",
                        onMinus = {
                            onUpdateSettings(settings.copy(cooldownMinutes = (settings.cooldownMinutes - 5).coerceAtLeast(1)))
                        },
                        onPlus = {
                            onUpdateSettings(settings.copy(cooldownMinutes = (settings.cooldownMinutes + 5).coerceAtMost(180)))
                        }
                    )
                }

                StepperRow(
                    label = "Giới hạn/ngày (hôm nay: $todayAnsweredCount)",
                    valueText = if (settings.dailyLimit <= 0) "Vô hạn" else "${settings.dailyLimit} câu",
                    onMinus = {
                        val next = (settings.dailyLimit - 5).coerceAtLeast(0)
                        onUpdateSettings(settings.copy(dailyLimit = next))
                    },
                    onPlus = {
                        val next = if (settings.dailyLimit <= 0) 10 else (settings.dailyLimit + 5).coerceAtMost(200)
                        onUpdateSettings(settings.copy(dailyLimit = next))
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Giờ im lặng ban đêm (23h – 6h)", fontWeight = FontWeight.Medium)
                        Text(
                            "Không bật câu hỏi trong khung giờ ngủ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.quietHoursEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(quietHoursEnabled = it)) }
                    )
                }
            }
        }

        // Word Selection Policy (multi-criteria)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tiêu chí chọn từ hiển thị",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                WordSelectionPolicy.entries.forEach { policy ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.selectionPolicy == policy,
                            onClick = { onUpdateSettings(settings.copy(selectionPolicy = policy)) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(policy.label, fontWeight = FontWeight.Medium)
                            Text(
                                policy.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (settings.selectionPolicy == WordSelectionPolicy.BY_TOPIC_OR_POS) {
                    Text("Lọc theo từ loại:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val posChoices = listOf("ALL" to "Tất cả") + PosTag.entries.map { it.code to it.code }
                        posChoices.forEach { (code, label) ->
                            FilterChip(
                                selected = settings.filterPos == code,
                                onClick = { onUpdateSettings(settings.copy(filterPos = code)) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Text("Lọc theo cấp độ CEFR:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "A1", "A2", "B1", "B2", "C1", "C2").forEach { cefr ->
                            FilterChip(
                                selected = settings.filterCefr == cefr,
                                onClick = { onUpdateSettings(settings.copy(filterCefr = cefr)) },
                                label = { Text(if (cefr == "ALL") "Tất cả" else cefr) }
                            )
                        }
                    }
                    if (filterMatchCount == 0) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Chưa có từ nào khớp bộ lọc hiện tại (0 từ). Hãy đổi Từ loại / CEFR hoặc thêm từ mới để không bị thiếu câu hỏi khi mở khóa.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Có $filterMatchCount từ phù hợp với bộ lọc hiện tại.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Question style (Anti-rote-learning)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Kiểu câu hỏi (Chống học vẹt)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                QuizQuestionStyle.entries.forEach { style ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.questionStyle == style,
                            onClick = { onUpdateSettings(settings.copy(questionStyle = style)) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(style.label, fontWeight = FontWeight.Medium)
                            Text(
                                style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Author & Anti-theft integrity certification card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (integrityStatus.isAuthentic) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)
            ),
            border = BorderStroke(
                1.dp,
                if (integrityStatus.isAuthentic) MaterialTheme.colorScheme.outlineVariant else Color(0xFFFCA5A5)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Bản quyền & Xác thực tác giả",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Nhóm tác giả:\n${integrityStatus.authorDisplay.ifBlank { OwnershipGuard.authorDisplay() }}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Định danh gói gốc: com.huy.unlockvocab • v${BuildConfig.VERSION_NAME} (${if (BuildConfig.DEBUG) "Debug" else "Release R8"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (integrityStatus.isAuthentic) {
                        "Chữ ký SHA-256: ${integrityStatus.certSha256Short} (Chính chủ • Đã xác thực SQLite & APK)"
                    } else {
                        "Chữ ký SHA-256: ${integrityStatus.certSha256Short} (Không khớp chữ ký gốc: ${integrityStatus.reason})"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (integrityStatus.isAuthentic) Color(0xFF15803D) else Color(0xFFB91C1C),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Dữ liệu từ điển mở: FVDP (GPL) • CEFR-J & NGSL (CC BY-SA 4.0) • ipa-dict (MIT)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun TriStatePermissionRow(
    title: String,
    granted: Boolean?,
    actionLabel: String,
    grantedText: String = "Đã cấp",
    deniedText: String = "Chưa cấp",
    unknownText: String = "Cần kiểm tra thủ công",
    onAction: () -> Unit
) {
    val (statusText, statusColor) = when (granted) {
        true -> grantedText to Color(0xFF15803D)
        false -> deniedText to Color(0xFFB91C1C)
        null -> unknownText to Color(0xFFB45309)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        }
        if (granted != true) {
            OutlinedButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
            ) {
                Text(actionLabel, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = onMinus,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 34.dp)
            ) { Text("-", maxLines = 1, softWrap = false) }
            Text(valueText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            OutlinedButton(
                onClick = onPlus,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 34.dp)
            ) { Text("+", maxLines = 1, softWrap = false) }
        }
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
