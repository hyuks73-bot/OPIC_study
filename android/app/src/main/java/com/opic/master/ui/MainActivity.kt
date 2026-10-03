package com.opic.master.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.lifecycle.lifecycleScope
import com.opic.master.data.local.AppDatabase
import com.opic.master.data.model.Sentence
import com.opic.master.data.sync.GitHubSyncWorker
import com.opic.master.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val HTML_TAG_REGEX = Regex("<.*?>")

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Seed database from bundled assets if empty
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.seedDatabaseIfEmpty(this@MainActivity, db.sentenceDao())
        }

        // 2. Trigger background differential GitHub Sync via WorkManager
        triggerGitHubSync()

        setContent {
            val sentences by db.sentenceDao().getAllSentences().collectAsState(initial = emptyList())
            val daysEntities by db.sentenceDao().getAllDays().collectAsState(initial = emptyList())
            val days = remember(daysEntities) {
                daysEntities.map { DayMeta(it.dayKey, it.tabLabel, it.title, it.emoji) }
            }
            val isPlaying by PlaybackService.isPlayingFlow.collectAsState()
            val currentPlayingId by PlaybackService.currentPlayingSentenceId.collectAsState()
            val isSyncing by GitHubSyncWorker.isSyncingFlow.collectAsState()

            // Real-time Galaxy Fold 8 screen configuration & hinge tracking
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp

            var foldingFeatureState by remember { mutableStateOf<FoldingFeature.State?>(null) }

            // 2. Track Fold 8 Hinge Posture via Jetpack WindowInfoTracker
            LaunchedEffect(Unit) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collectLatest { layoutInfo ->
                        val foldFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                        foldingFeatureState = foldFeature?.state
                    }
            }

            // Reliable posture evaluation:
            // - If screen width is narrow (< 600dp), it is ALWAYS the Cover Screen (Folded)
            // - If screen width is wide (>= 600dp), it is Unfolded (Dual Pane Studio or Flex Mode)
            val isCoverScreen = screenWidthDp < 600
            val isFlexMode = !isCoverScreen && (foldingFeatureState == FoldingFeature.State.HALF_OPENED)
            val isUnfolded = !isCoverScreen

            Fold8AdaptiveApp(
                sentences = sentences,
                days = days,
                isUnfolded = isUnfolded,
                isFlexMode = isFlexMode,
                isPlaying = isPlaying,
                currentPlayingId = currentPlayingId,
                isSyncing = isSyncing,
                onPlaySentence = { sentence, repeatCount, speeds ->
                    playSentenceViaService(sentence, repeatCount, speeds)
                },
                onPlayAll = { daySentences, repeatCount, speeds ->
                    playAllViaService(daySentences, repeatCount, speeds)
                },
                onTogglePlay = {
                    togglePlayback()
                },
                onStop = {
                    stopPlayback()
                },
                onUpdateSettings = { newRepeat, speeds ->
                    updatePlaybackSettings(newRepeat, speeds)
                },
                onSyncGitHub = {
                    triggerGitHubSync()
                }
            )
        }
    }

    private fun triggerGitHubSync() {
        if (GitHubSyncWorker.isSyncingFlow.value) {
            android.widget.Toast.makeText(this, "이미 최신 데이터 동기화가 진행 중입니다...", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        android.widget.Toast.makeText(this, "🔄 GitHub 최신 학습 데이터 동기화 시작...", android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            val success = GitHubSyncWorker.performSync(applicationContext)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                if (success) {
                    android.widget.Toast.makeText(this@MainActivity, "✅ 동기화 완료! 새로운 학습 세트가 반영되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "⚠️ 동기화 실패: 네트워크 상태를 확인해 주세요.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playSentenceViaService(sentence: Sentence, repeatCount: Int, speeds: List<Float> = emptyList()) {
        val path = sentence.localAudioPath ?: "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main/${sentence.audioUrl}"
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY_SENTENCE
            putExtra(PlaybackService.EXTRA_AUDIO_PATH, path)
            putExtra(PlaybackService.EXTRA_SENTENCE_TITLE, sentence.en.replace(HTML_TAG_REGEX, ""))
            putExtra(PlaybackService.EXTRA_SENTENCE_ID, sentence.id)
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, repeatCount)
            if (speeds.isNotEmpty()) {
                putExtra(PlaybackService.EXTRA_REPEAT_SPEEDS, speeds.toFloatArray())
            }
        }
        startForegroundService(intent)
    }

    private fun playAllViaService(sentences: List<Sentence>, repeatCount: Int, speeds: List<Float> = emptyList()) {
        if (sentences.isEmpty()) return
        val paths = ArrayList(sentences.map { it.localAudioPath ?: "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main/${it.audioUrl}" })
        val titles = ArrayList(sentences.map { it.en.replace(HTML_TAG_REGEX, "") })
        val ids = ArrayList(sentences.map { it.id })

        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY_ALL
            putStringArrayListExtra(PlaybackService.EXTRA_AUDIO_PATHS, paths)
            putStringArrayListExtra(PlaybackService.EXTRA_SENTENCE_TITLES, titles)
            putStringArrayListExtra(PlaybackService.EXTRA_SENTENCE_IDS, ids)
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, repeatCount)
            if (speeds.isNotEmpty()) {
                putExtra(PlaybackService.EXTRA_REPEAT_SPEEDS, speeds.toFloatArray())
            }
        }
        startForegroundService(intent)
    }

    private fun togglePlayback() {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_TOGGLE_PLAY
        }
        startService(intent)
    }

    private fun stopPlayback() {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_STOP
        }
        startService(intent)
    }

    private fun updatePlaybackSettings(repeatCount: Int, speeds: List<Float>) {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_UPDATE_SETTINGS
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, repeatCount)
            putExtra(PlaybackService.EXTRA_REPEAT_SPEEDS, speeds.toFloatArray())
        }
        startService(intent)
    }
}
