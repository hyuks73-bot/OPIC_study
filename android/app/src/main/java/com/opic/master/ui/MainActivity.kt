package com.opic.master.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.opic.master.data.local.AppDatabase
import com.opic.master.data.model.Sentence
import com.opic.master.data.sync.GitHubSyncWorker
import com.opic.master.service.PlaybackService
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Trigger background differential GitHub Sync via WorkManager
        triggerGitHubSync()

        setContent {
            val sentences by db.sentenceDao().getAllSentences().collectAsState(initial = emptyList())
            val isPlaying by PlaybackService.isPlayingFlow.collectAsState()
            val currentPlayingId by PlaybackService.currentPlayingSentenceId.collectAsState()

            var isUnfolded by remember { mutableStateOf(false) }
            var isFlexMode by remember { mutableStateOf(false) }

            // 2. Track Fold 8 Fold/Unfold & Hinge Posture via Jetpack WindowInfoTracker
            LaunchedEffect(Unit) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collectLatest { layoutInfo ->
                        val foldFeature = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                        if (foldFeature != null) {
                            isUnfolded = true
                            isFlexMode = foldFeature.state == FoldingFeature.State.HALF_OPENED
                        } else {
                            // If width is wide (> 600dp), it is the unfolded main screen
                            val widthDp = resources.configuration.screenWidthDp
                            isUnfolded = widthDp > 600
                            isFlexMode = false
                        }
                    }
            }

            Fold8AdaptiveApp(
                sentences = sentences,
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
                onPrev = {
                    val intent = Intent(this, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_PREV
                    }
                    startService(intent)
                },
                onNext = {
                    val intent = Intent(this, PlaybackService::class.java).apply {
                        action = PlaybackService.ACTION_NEXT
                    }
                    startService(intent)
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
            putExtra(PlaybackService.EXTRA_SENTENCE_TITLE, sentence.en.replace(Regex("<.*?>"), ""))
            putExtra(PlaybackService.EXTRA_SENTENCE_ID, sentence.id)
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, repeatCount)
        }
        startForegroundService(intent)
    }

    private fun playAllViaService(sentences: List<Sentence>, repeatCount: Int) {
        if (sentences.isEmpty()) return
        val paths = ArrayList(sentences.map { it.localAudioPath ?: "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main/${it.audioUrl}" })
        val titles = ArrayList(sentences.map { it.en.replace(Regex("<.*?>"), "") })
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
}
