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
                onPlaySentence = { sentence, repeatCount ->
                    playSentenceViaService(sentence, repeatCount)
                },
                onPlayAll = { daySentences, repeatCount ->
                    playAllViaService(daySentences, repeatCount)
                },
                onTogglePlay = {
                    togglePlayback()
                },
                onStop = {
                    stopPlayback()
                },
                onUpdateRepeatCount = { newRepeat ->
                    updateRepeatCount(newRepeat)
                },
                onUpdateSpeed = { speed ->
                    updatePlaybackSpeed(speed)
                },
                onSyncGitHub = {
                    triggerGitHubSync()
                }
            )
        }
    }

    private fun triggerGitHubSync() {
        val syncRequest = OneTimeWorkRequestBuilder<GitHubSyncWorker>().build()
        WorkManager.getInstance(this).enqueue(syncRequest)
    }

    private fun playSentenceViaService(sentence: Sentence, repeatCount: Int) {
        val path = sentence.localAudioPath ?: "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main/${sentence.audioUrl}"
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY_SENTENCE
            putExtra(PlaybackService.EXTRA_AUDIO_PATH, path)
            putExtra(PlaybackService.EXTRA_SENTENCE_TITLE, sentence.en.replace(HTML_TAG_REGEX, ""))
            putExtra(PlaybackService.EXTRA_SENTENCE_ID, sentence.id)
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, repeatCount)
        }
        startForegroundService(intent)
    }

    private fun playAllViaService(sentences: List<Sentence>, repeatCount: Int) {
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

    private fun updateRepeatCount(repeatCount: Int) {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_UPDATE_REPEAT_COUNT
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, repeatCount)
        }
        startService(intent)
    }

    private fun updatePlaybackSpeed(speed: Float) {
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_SET_SPEED
            putExtra(PlaybackService.EXTRA_SPEED, speed)
        }
        startService(intent)
    }
}
