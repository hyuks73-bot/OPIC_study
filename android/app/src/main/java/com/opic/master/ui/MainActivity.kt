package com.opic.master.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.opic.master.data.local.AppDatabase
import com.opic.master.data.model.Sentence
import com.opic.master.data.sync.GitHubSyncWorker
import com.opic.master.service.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Seed database from bundled assets if empty
        lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.seedDatabaseIfEmpty(this@MainActivity, db.sentenceDao())
        }

        // 2. Trigger background differential GitHub Sync
        triggerGitHubSync()

        setContent {
            val sentences by db.sentenceDao().getAllSentences().collectAsState(initial = emptyList())
            val daysEntities by db.sentenceDao().getAllDays().collectAsState(initial = emptyList())
            val days = remember(daysEntities) {
                daysEntities.map { DayMeta(it.dayKey, it.tabLabel, it.title, it.emoji) }
            }
            val isPlaying by PlaybackService.isPlayingFlow.collectAsState()
            val currentPlayingId by PlaybackService.currentPlayingSentenceId.collectAsState()
            val currentRepeatIndex by PlaybackService.currentRepeatFlow.collectAsState()
            val isSyncing by GitHubSyncWorker.isSyncingFlow.collectAsState()

            val currentView = LocalView.current
            LaunchedEffect(isPlaying) {
                currentView.keepScreenOn = isPlaying
            }

            // Real-time Galaxy Fold 8 screen configuration & hinge tracking
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp

            var foldingFeatureState by remember { mutableStateOf<FoldingFeature.State?>(null) }

            // 2. Track Fold 8 Hinge Posture via Jetpack WindowInfoTracker
            LaunchedEffect(Unit) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collectLatest { layoutInfo ->
                        val foldFeature = layoutInfo.displayFeatures.firstNotNullOfOrNull { it as? FoldingFeature }
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
                currentRepeatIndex = currentRepeatIndex,
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
                onSetTemporarySpeed = { speed ->
                    setTemporarySpeed(speed)
                },
                onSyncGitHub = {
                    triggerGitHubSync()
                },
            )
        }
    }

    private fun triggerGitHubSync() {
        if (GitHubSyncWorker.isSyncingFlow.value) {
            Toast.makeText(this, "이미 최신 데이터 동기화가 진행 중입니다...", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(this, "🔄 GitHub 최신 학습 데이터 동기화 시작...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            val success = GitHubSyncWorker.performSync(applicationContext)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@MainActivity, "✅ 동기화 완료! 새로운 학습 세트가 반영되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "⚠️ 동기화 실패: 네트워크 상태를 확인해 주세요.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun playSentenceViaService(sentence: Sentence, repeatCount: Int, speeds: List<Float> = emptyList()) {
        val path = sentence.localAudioPath ?: "${GitHubSyncWorker.GITHUB_RAW_BASE}/${sentence.audioUrl}"
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY_SENTENCE
            putExtra(PlaybackService.EXTRA_AUDIO_PATH, path)
            putExtra(PlaybackService.EXTRA_SENTENCE_TITLE, cleanSentenceText(sentence.en))
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
        val paths = ArrayList<String>(sentences.size)
        val titles = ArrayList<String>(sentences.size)
        val ids = ArrayList<String>(sentences.size)

        for (s in sentences) {
            paths.add(s.localAudioPath ?: "${GitHubSyncWorker.GITHUB_RAW_BASE}/${s.audioUrl}")
            titles.add(cleanSentenceText(s.en))
            ids.add(s.id)
        }

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

    private fun setTemporarySpeed(speed: Float) {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_SET_TEMPORARY_SPEED
            putExtra(PlaybackService.EXTRA_TEMPORARY_SPEED, speed)
        }
        startService(intent)
    }
}
