package com.opic.master.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.opic.master.data.local.AppDatabase
import com.opic.master.data.model.ManifestResponse
import com.opic.master.data.model.Sentence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class GitHubSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main"
        const val MANIFEST_URL = "$GITHUB_RAW_BASE/data_manifest.json"

        private val _isSyncingFlow = MutableStateFlow(false)
        val isSyncingFlow = _isSyncingFlow.asStateFlow()

        suspend fun performSync(context: Context): Boolean = withContext(Dispatchers.IO) {
            _isSyncingFlow.value = true
            try {
                val client = OkHttpClient()
                val gson = Gson()
                val dao = AppDatabase.getDatabase(context).sentenceDao()

                // 1. Fetch manifest.json safely with auto-closeable response
                val request = Request.Builder().url(MANIFEST_URL).build()
                val manifest = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful || response.body == null) {
                        return@withContext false
                    }
                    gson.fromJson(response.body!!.string(), ManifestResponse::class.java)
                }

                // 2. IMMEDIATELY Save Days & clean up deleted Days from Room Database
                val activeDayKeys = manifest.days.keys.toList()
                if (activeDayKeys.isNotEmpty()) {
                    var dayOrderCounter = 0
                    val dayEntities = manifest.days.map { (dayKey, dayData) ->
                        AppDatabase.createDayEntity(dayKey, dayData.title, dayOrderCounter++)
                    }
                    dao.insertDays(dayEntities)
                    dao.deleteDaysNotIn(activeDayKeys)
                    dao.deleteSentencesNotInDays(activeDayKeys)
                }

                // 3. Prepare audio directory & existing records to preserve user settings
                val audioDir = File(context.filesDir, "audio").apply {
                    if (!exists()) mkdirs()
                }
                val existingSentences = dao.getAllSentencesList().associateBy { it.id }

                val sentenceEntities = mutableListOf<Sentence>()
                var syncOrderCounter = 0

                for ((dayKey, dayData) in manifest.days) {
                    for (item in dayData.sentences) {
                        val audioFile = File(audioDir, "${item.id}.mp3")
                        val isDownloaded = audioFile.exists() && audioFile.length() > 0
                        val existing = existingSentences[item.id]

                        sentenceEntities.add(
                            Sentence(
                                id = item.id,
                                dayKey = dayKey,
                                orderIndex = syncOrderCounter++,
                                en = item.en,
                                ko = item.ko,
                                guide = item.guide,
                                tip = item.tip,
                                audioUrl = item.audioUrl,
                                imageUrl = item.imageUrl,
                                localAudioPath = if (isDownloaded) audioFile.absolutePath else null,
                                localImagePath = existing?.localImagePath,
                                isDownloaded = isDownloaded,
                                bookmark = existing?.bookmark ?: false,
                                repeatCount = existing?.repeatCount ?: 3
                            )
                        )
                    }
                }

                // IMMEDIATELY Save Sentences & clean up removed sentences
                dao.insertAll(sentenceEntities)
                val activeSentenceIds = sentenceEntities.map { it.id }
                if (activeSentenceIds.isNotEmpty()) {
                    dao.deleteSentencesNotIn(activeSentenceIds)
                }

                // Clean up orphaned audio files for deleted sentences
                val removedSentenceIds = existingSentences.keys - activeSentenceIds.toSet()
                for (removedId in removedSentenceIds) {
                    val orphanedFile = File(audioDir, "$removedId.mp3")
                    if (orphanedFile.exists()) {
                        orphanedFile.delete()
                    }
                }

                // 4. Download any missing MP3 audio files in the background
                var allAudioDownloaded = true
                for (sentence in sentenceEntities) {
                    val audioFile = File(audioDir, "${sentence.id}.mp3")
                    if (!audioFile.exists() || audioFile.length() == 0L) {
                        val audioUrl = "$GITHUB_RAW_BASE/${sentence.audioUrl}"
                        val audioReq = Request.Builder().url(audioUrl).build()
                        try {
                            client.newCall(audioReq).execute().use { audioResp ->
                                if (audioResp.isSuccessful && audioResp.body != null) {
                                    FileOutputStream(audioFile).use { output ->
                                        audioResp.body!!.byteStream().copyTo(output)
                                    }
                                    dao.updateAudioDownloaded(sentence.id, audioFile.absolutePath)
                                } else {
                                    allAudioDownloaded = false
                                }
                            }
                        } catch (e: Exception) {
                            allAudioDownloaded = false
                        }
                    }
                }

                allAudioDownloaded
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {
                _isSyncingFlow.value = false
            }
        }
    }

    override suspend fun doWork(): Result {
        val success = performSync(applicationContext)
        return if (success) Result.success() else Result.retry()
    }
}
