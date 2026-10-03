package com.opic.master.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.opic.master.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var repeatTargetCount = 3
    private var currentRepeat = 0
    private var repeatSpeeds = listOf(1.0f, 1.0f, 1.0f)
    private var isShadowingPauseActive = false
    private var shadowingJob: Job? = null

    // Playlist Mode State
    private var isPlaylistMode = false
    private var playlistPaths = listOf<String>()
    private var playlistTitles = listOf<String>()
    private var playlistIds = listOf<String>()
    private var currentPlaylistIndex = 0

    private fun cancelShadowingPause() {
        shadowingJob?.cancel()
        shadowingJob = null
        isShadowingPauseActive = false
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "opic_shadowing_channel"

        const val ACTION_PLAY_SENTENCE = "ACTION_PLAY_SENTENCE"
        const val ACTION_PLAY_ALL = "ACTION_PLAY_ALL"
        const val ACTION_TOGGLE_PLAY = "ACTION_TOGGLE_PLAY"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_SETTINGS = "ACTION_UPDATE_SETTINGS"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_NEXT = "ACTION_NEXT"

        const val EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH"
        const val EXTRA_SENTENCE_TITLE = "EXTRA_SENTENCE_TITLE"
        const val EXTRA_SENTENCE_ID = "EXTRA_SENTENCE_ID"
        const val EXTRA_REPEAT_COUNT = "EXTRA_REPEAT_COUNT"
        const val EXTRA_REPEAT_SPEEDS = "EXTRA_REPEAT_SPEEDS"

        const val EXTRA_AUDIO_PATHS = "EXTRA_AUDIO_PATHS"
        const val EXTRA_SENTENCE_TITLES = "EXTRA_SENTENCE_TITLES"
        const val EXTRA_SENTENCE_IDS = "EXTRA_SENTENCE_IDS"

        // Reactive StateFlows for Compose UI
        private val _isPlayingFlow = MutableStateFlow(false)
        val isPlayingFlow = _isPlayingFlow.asStateFlow()

        private val _currentPlayingSentenceId = MutableStateFlow<String?>(null)
        val currentPlayingSentenceId = _currentPlayingSentenceId.asStateFlow()

        private val _currentPlayingTitle = MutableStateFlow("")
        val currentPlayingTitle = _currentPlayingTitle.asStateFlow()

        private val _currentRepeatFlow = MutableStateFlow(1)
        val currentRepeatFlow = _currentRepeatFlow.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 1. Initialize ExoPlayer with AudioAttributes for Speech & CPU WakeLock
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            // Screen-Off Continuous Playback: Prevents CPU from sleeping when screen turns off
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // 2. Custom Player Listener for Shadowing Repeat with 1.2s Pause & Playlist Advance
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayingFlow.value = isPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                cancelShadowingPause()
                if (isPlaylistMode && currentPlaylistIndex + 1 < playlistPaths.size) {
                    currentPlaylistIndex++
                    playCurrentPlaylistItem()
                } else {
                    _isPlayingFlow.value = false
                    _currentPlayingSentenceId.value = null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentRepeat++
                    _currentRepeatFlow.value = currentRepeat + 1

                    if (currentRepeat < repeatTargetCount) {
                        // 1.2-second smart pause for learner's vocal shadowing
                        cancelShadowingPause()
                        isShadowingPauseActive = true
                        val nextSpeed = repeatSpeeds.getOrElse(currentRepeat) { 1.0f }
                        player.setPlaybackSpeed(nextSpeed)
                        shadowingJob = serviceScope.launch {
                            delay(1200)
                            if (isShadowingPauseActive && player.playbackState == Player.STATE_ENDED) {
                                player.seekTo(0)
                                player.play()
                                isShadowingPauseActive = false
                            }
                        }
                    } else {
                        // Current sentence repeats completed
                        currentRepeat = 0
                        _currentRepeatFlow.value = 1
                        cancelShadowingPause()

                        if (isPlaylistMode) {
                            if (currentPlaylistIndex + 1 < playlistPaths.size) {
                                currentPlaylistIndex++
                                playCurrentPlaylistItem()
                            } else {
                                // Entire tab playlist complete
                                _isPlayingFlow.value = false
                                _currentPlayingSentenceId.value = null
                                stopForeground(STOP_FOREGROUND_REMOVE)
                            }
                        } else {
                            _isPlayingFlow.value = false
                        }
                    }
                }
            }
        })

        // 3. Create MediaSession for Lockscreen, AOD, and Bluetooth Controls
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PLAY_SENTENCE -> {
                val audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH) ?: return rejectStartRequest()
                cancelShadowingPause()
                isPlaylistMode = false
                val title = intent.getStringExtra(EXTRA_SENTENCE_TITLE) ?: "OPIc Sentence"
                val sentenceId = intent.getStringExtra(EXTRA_SENTENCE_ID)
                repeatTargetCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 3)
                val speedsExtra = intent.getFloatArrayExtra(EXTRA_REPEAT_SPEEDS)
                if (speedsExtra != null && speedsExtra.isNotEmpty()) {
                    repeatSpeeds = speedsExtra.toList()
                } else if (repeatSpeeds.size != repeatTargetCount) {
                    repeatSpeeds = List(repeatTargetCount) { 1.0f }
                }
                currentRepeat = 0
                _currentRepeatFlow.value = 1

                _currentPlayingSentenceId.value = sentenceId
                _currentPlayingTitle.value = title

                val mediaItem = MediaItem.fromUri(audioPath)
                player.setMediaItem(mediaItem)
                player.prepare()
                val initialSpeed = repeatSpeeds.getOrElse(0) { 1.0f }
                player.setPlaybackSpeed(initialSpeed)
                player.play()

                startForeground(NOTIFICATION_ID, buildNotification(title, currentSubtitle()))
            }

            ACTION_PLAY_ALL -> {
                val paths = intent.getStringArrayListExtra(EXTRA_AUDIO_PATHS)
                val titles = intent.getStringArrayListExtra(EXTRA_SENTENCE_TITLES)
                val ids = intent.getStringArrayListExtra(EXTRA_SENTENCE_IDS)
                if (paths.isNullOrEmpty() || titles == null || ids == null) return rejectStartRequest()
                cancelShadowingPause()

                isPlaylistMode = true
                playlistPaths = paths
                playlistTitles = titles
                playlistIds = ids
                currentPlaylistIndex = 0
                repeatTargetCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 3)
                val speedsExtra = intent.getFloatArrayExtra(EXTRA_REPEAT_SPEEDS)
                if (speedsExtra != null && speedsExtra.isNotEmpty()) {
                    repeatSpeeds = speedsExtra.toList()
                } else if (repeatSpeeds.size != repeatTargetCount) {
                    repeatSpeeds = List(repeatTargetCount) { 1.0f }
                }
                currentRepeat = 0
                _currentRepeatFlow.value = 1

                playCurrentPlaylistItem()
            }

            ACTION_TOGGLE_PLAY -> {
                if (player.isPlaying) {
                    cancelShadowingPause()
                    player.pause()
                } else if (player.playbackState == Player.STATE_ENDED) {
                    cancelShadowingPause()
                    player.setPlaybackSpeed(repeatSpeeds.getOrElse(currentRepeat) { 1.0f })
                    player.seekTo(0)
                    player.play()
                } else {
                    player.play()
                }
            }

            ACTION_STOP -> {
                cancelShadowingPause()
                isPlaylistMode = false
                currentRepeat = 0
                player.stop()
                player.clearMediaItems()
                _currentPlayingSentenceId.value = null
                _currentPlayingTitle.value = ""
                _currentRepeatFlow.value = 1
                stopForeground(STOP_FOREGROUND_REMOVE)
            }

            ACTION_UPDATE_SETTINGS -> {
                val newCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, repeatTargetCount)
                val speedsExtra = intent.getFloatArrayExtra(EXTRA_REPEAT_SPEEDS)
                repeatTargetCount = newCount
                if (speedsExtra != null && speedsExtra.isNotEmpty()) {
                    repeatSpeeds = speedsExtra.toList()
                }
                val currentSpeed = repeatSpeeds.getOrElse(currentRepeat) { 1.0f }
                player.setPlaybackSpeed(currentSpeed)
                if (player.isPlaying) {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager?.notify(NOTIFICATION_ID, buildNotification(_currentPlayingTitle.value, currentSubtitle()))
                }
            }

            ACTION_PAUSE -> {
                cancelShadowingPause()
                player.pause()
            }

            ACTION_RESUME -> {
                player.play()
            }

            ACTION_PREV -> {
                cancelShadowingPause()
                if (isPlaylistMode && currentPlaylistIndex > 0) {
                    currentPlaylistIndex--
                    currentRepeat = 0
                    playCurrentPlaylistItem()
                } else {
                    player.setPlaybackSpeed(repeatSpeeds.getOrElse(currentRepeat) { 1.0f })
                    player.seekTo(0)
                }
            }

            ACTION_NEXT -> {
                cancelShadowingPause()
                if (isPlaylistMode && currentPlaylistIndex + 1 < playlistPaths.size) {
                    currentPlaylistIndex++
                    currentRepeat = 0
                    playCurrentPlaylistItem()
                }
            }
        }

        return START_STICKY
    }

    private fun playCurrentPlaylistItem() {
        if (currentPlaylistIndex !in playlistPaths.indices) return
        cancelShadowingPause()

        val audioPath = playlistPaths[currentPlaylistIndex]
        val title = playlistTitles.getOrNull(currentPlaylistIndex) ?: "OPIc Sentence"
        val sentenceId = playlistIds.getOrNull(currentPlaylistIndex)

        _currentPlayingSentenceId.value = sentenceId
        _currentPlayingTitle.value = title
        currentRepeat = 0
        _currentRepeatFlow.value = 1

        val mediaItem = MediaItem.fromUri(audioPath)
        player.setMediaItem(mediaItem)
        player.prepare()
        val initialSpeed = repeatSpeeds.getOrElse(0) { 1.0f }
        player.setPlaybackSpeed(initialSpeed)
        player.play()

        startForeground(NOTIFICATION_ID, buildNotification(title, currentSubtitle()))
    }

    private fun currentSubtitle(): String =
        if (isPlaylistMode) "탭 전체 재생 [${currentPlaylistIndex + 1}/${playlistPaths.size}] · 반복 ${repeatTargetCount}회"
        else "반복: ${repeatTargetCount}회 · 화면 꺼짐 연속 재생"

    /**
     * Play requests arrive via startForegroundService(), which obliges the service to call
     * startForeground() within a few seconds even when the request is invalid — otherwise the
     * system kills the app (ForegroundServiceDidNotStartInTimeException). Satisfy that contract,
     * and leave the foreground state only if nothing is actually playing.
     */
    private fun rejectStartRequest(): Int {
        val playing = player.isPlaying
        val title = _currentPlayingTitle.value.ifEmpty { "OPIc Sentence" }
        val subtitle = if (playing) currentSubtitle() else "재생할 음원을 찾을 수 없습니다"
        startForeground(NOTIFICATION_ID, buildNotification(title, subtitle))
        if (!playing) stopForeground(STOP_FOREGROUND_REMOVE)
        return START_STICKY
    }

    private fun buildNotification(title: String, subtitle: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OPIc 섀도잉: $title")
            .setContentText(subtitle)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OPIc Shadowing Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "화면이 꺼진 상태에서도 문장 섀도잉 음원을 무중단 연속 재생합니다."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        cancelShadowingPause()
        serviceScope.cancel()
        _isPlayingFlow.value = false
        _currentPlayingSentenceId.value = null
        _currentPlayingTitle.value = ""
        _currentRepeatFlow.value = 1
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
