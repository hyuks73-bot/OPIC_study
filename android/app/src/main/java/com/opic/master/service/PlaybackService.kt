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
import com.opic.master.R
import com.opic.master.ui.MainActivity
import kotlinx.coroutines.*

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var repeatTargetCount = 3
    private var currentRepeat = 0
    private var isShadowingPauseActive = false

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "opic_shadowing_channel"
        const val ACTION_PLAY_SENTENCE = "ACTION_PLAY_SENTENCE"
        const val EXTRA_AUDIO_PATH = "EXTRA_AUDIO_PATH"
        const val EXTRA_SENTENCE_TITLE = "EXTRA_SENTENCE_TITLE"
        const val EXTRA_REPEAT_COUNT = "EXTRA_REPEAT_COUNT"
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

        // 2. Custom Player Listener for Shadowing Repeat with 1.2s Pause
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentRepeat++
                    if (currentRepeat < repeatTargetCount) {
                        // 1.2-second smart pause for learner's vocal shadowing
                        serviceScope.launch {
                            isShadowingPauseActive = true
                            delay(1200)
                            if (isShadowingPauseActive) {
                                player.seekTo(0)
                                player.play()
                                isShadowingPauseActive = false
                            }
                        }
                    } else {
                        // Loop complete
                        currentRepeat = 0
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
        
        val action = intent?.action
        if (action == ACTION_PLAY_SENTENCE) {
            val audioPath = intent.getStringExtra(EXTRA_AUDIO_PATH) ?: return START_STICKY
            val title = intent.getStringExtra(EXTRA_SENTENCE_TITLE) ?: "OPIc Sentence"
            repeatTargetCount = intent.getIntExtra(EXTRA_REPEAT_COUNT, 3)
            currentRepeat = 0
            isShadowingPauseActive = false

            val mediaItem = MediaItem.fromUri(audioPath)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()

            startForeground(NOTIFICATION_ID, buildNotification(title))
        }

        return START_STICKY
    }

    private fun buildNotification(title: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OPIc 섀도잉: $title")
            .setContentText("반복: ${repeatTargetCount}회 · 화면 꺼짐 연속 재생 중")
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
                description = "화면이 꺼진 상태에서도 문장 섀도잉 음원을 재생합니다."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
