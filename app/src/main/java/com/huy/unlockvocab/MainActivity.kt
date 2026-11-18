package com.huy.unlockvocab

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.huy.unlockvocab.data.VocabRepository
import com.huy.unlockvocab.data.db.StatsSummary
import com.huy.unlockvocab.service.UnlockService
import com.huy.unlockvocab.ui.home.HomeScreen
import com.huy.unlockvocab.ui.home.hasNotificationPermission
import com.huy.unlockvocab.ui.home.isIgnoringBatteryOptimizations
import com.huy.unlockvocab.ui.stats.StatsScreen
import com.huy.unlockvocab.ui.vocab.VocabScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale.US)
                ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val scope = rememberCoroutineScope()
                val repository = remember { VocabRepository.getInstance(context) }

                var currentTab by remember { mutableIntStateOf(0) }
                var settings by remember { mutableStateOf(repository.settingsStore.load()) }
                var stats by remember { mutableStateOf(StatsSummary()) }
                var snoozeRemainingMinutes by remember {
                    mutableIntStateOf(repository.settingsStore.getSnoozeRemainingMinutes())
                }
                var todayAnsweredCount by remember { mutableIntStateOf(0) }
                var filterMatchCount by remember { mutableIntStateOf(0) }
                var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
                var hasNotif by remember { mutableStateOf(hasNotificationPermission(context)) }
                var ignoringBattery by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasNotif = granted
                }

                fun refreshAll() {
                    hasOverlay = Settings.canDrawOverlays(context)
                    hasNotif = hasNotificationPermission(context)
                    ignoringBattery = isIgnoringBatteryOptimizations(context)
                    snoozeRemainingMinutes = repository.settingsStore.getSnoozeRemainingMinutes()
                    scope.launch {
                        val (s, today, matchCount) = withContext(Dispatchers.IO) {
                            Triple(
                                repository.getStatsSummary(),
                                repository.getTodayAnsweredCount(),
                                repository.countMatchingFilter(settings.filterPos, settings.filterCefr)
                            )
                        }
                        stats = s
                        todayAnsweredCount = today
                        filterMatchCount = matchCount
                    }
                    if (settings.serviceEnabled && hasOverlay) {
                        UnlockService.start(context)
                    }
                }

                LaunchedEffect(Unit) {
                    refreshAll()
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            refreshAll()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Text("🏠") },
                                label = { Text("Trang chủ") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Text("📚") },
                                label = { Text("Từ vựng") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = {
                                    currentTab = 2
                                    refreshAll()
                                },
                                icon = { Text("📊") },
                                label = { Text("Thống kê") }
                            )
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        when (currentTab) {
                            0 -> HomeScreen(
                                settings = settings,
                                stats = stats,
                                snoozeRemainingMinutes = snoozeRemainingMinutes,
                                todayAnsweredCount = todayAnsweredCount,
                                filterMatchCount = filterMatchCount,
                                hasOverlayPermission = hasOverlay,
                                hasNotificationPermission = hasNotif,
                                isIgnoringBatteryOpt = ignoringBattery,
                                onRequestNotificationPermission = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onUpdateSettings = { updated ->
                                    settings = updated
                                    repository.settingsStore.save(updated)
                                    snoozeRemainingMinutes = repository.settingsStore.getSnoozeRemainingMinutes()
                                    scope.launch {
                                        filterMatchCount = withContext(Dispatchers.IO) {
                                            repository.countMatchingFilter(updated.filterPos, updated.filterCefr)
                                        }
                                    }
                                    if (updated.serviceEnabled) {
                                        UnlockService.start(context)
                                    } else {
                                        UnlockService.stop(context)
                                    }
                                },
                                onSnooze30Minutes = {
                                    UnlockService.snooze30Minutes(context)
                                    snoozeRemainingMinutes = repository.settingsStore.getSnoozeRemainingMinutes()
                                },
                                onResumeFromSnooze = {
                                    UnlockService.resumeFromSnooze(context)
                                    snoozeRemainingMinutes = 0
                                },
                                onTestOverlayNow = {
                                    if (!Settings.canDrawOverlays(context)) {
                                        Toast.makeText(
                                            context,
                                            "Hãy cấp quyền Hiển thị trên ứng dụng khác trước.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        context.startActivity(
                                            Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                        )
                                    } else {
                                        UnlockService.triggerTestQuiz(context)
                                    }
                                }
                            )

                            1 -> VocabScreen(
                                repository = repository,
                                onSpeakWord = { w ->
                                    if (ttsReady) {
                                        tts?.speak(w, TextToSpeech.QUEUE_FLUSH, null, "vocab_$w")
                                    }
                                },
                                onWordListChanged = { refreshAll() }
                            )

                            2 -> StatsScreen(
                                repository = repository,
                                stats = stats
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
