package com.opic.master.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.opic.master.data.local.AppDatabase
import com.opic.master.data.model.Sentence
import com.opic.master.data.sync.GitHubSyncWorker
import com.opic.master.service.PlaybackService
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Trigger background differential GitHub Sync via WorkManager
        triggerGitHubSync()

        setContent {
            val sentences by db.sentenceDao().getAllSentences().collectAsState(initial = emptyList())
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
                onPlaySentence = { sentence ->
                    playSentenceViaService(sentence)
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

    private fun playSentenceViaService(sentence: Sentence) {
        // Prefer local offline MP3 path if downloaded, fallback to assets/network
        val path = sentence.localAudioPath ?: "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main/${sentence.audioUrl}"
        val intent = Intent(this, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_PLAY_SENTENCE
            putExtra(PlaybackService.EXTRA_AUDIO_PATH, path)
            putExtra(PlaybackService.EXTRA_SENTENCE_TITLE, sentence.en.replace(Regex("<.*?>"), ""))
            putExtra(PlaybackService.EXTRA_REPEAT_COUNT, sentence.repeatCount)
        }
        startForegroundService(intent)
    }
}
