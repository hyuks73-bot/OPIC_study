package com.opic.master.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.opic.master.data.local.AppDatabase
import com.opic.master.data.model.ManifestResponse
import com.opic.master.data.model.Sentence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class GitHubSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val dao = AppDatabase.getDatabase(appContext).sentenceDao()

    companion object {
        // Base Raw GitHub Content URL
        const val GITHUB_RAW_BASE = "https://raw.githubusercontent.com/hyuks73-bot/OPIC_study/main"
        const val MANIFEST_URL = "$GITHUB_RAW_BASE/data_manifest.json"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch manifest.json
            val request = Request.Builder().url(MANIFEST_URL).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                return@withContext Result.retry()
            }

            val jsonString = response.body!!.string()
            val manifest = gson.fromJson(jsonString, ManifestResponse::class.java)

            // 2. Prepare audio storage directory
            val audioDir = File(applicationContext.filesDir, "audio").apply {
                if (!exists()) mkdirs()
            }

            val sentenceEntities = mutableListOf<Sentence>()

            for ((dayKey, dayData) in manifest.days) {
                for (item in dayData.sentences) {
                    val audioFile = File(audioDir, "${item.id}.mp3")
                    var isDownloaded = audioFile.exists() && audioFile.length() > 0

                    // 3. Download MP3 if missing or updated
                    if (!isDownloaded) {
                        val audioUrl = "$GITHUB_RAW_BASE/${item.audioUrl}"
                        val audioReq = Request.Builder().url(audioUrl).build()
                        try {
                            val audioResp = client.newCall(audioReq).execute()
                            if (audioResp.isSuccessful && audioResp.body != null) {
                                FileOutputStream(audioFile).use { output ->
                                    audioResp.body!!.byteStream().copyTo(output)
                                }
                                isDownloaded = true
                            }
                        } catch (e: Exception) {
                            // Audio download can retry later
                        }
                    }

                    sentenceEntities.add(
                        Sentence(
                            id = item.id,
                            dayKey = dayKey,
                            en = item.en,
                            ko = item.ko,
                            guide = item.guide,
                            tip = item.tip,
                            audioUrl = item.audioUrl,
                            imageUrl = item.imageUrl,
                            localAudioPath = if (isDownloaded) audioFile.absolutePath else null,
                            isDownloaded = isDownloaded
                        )
                    )
                }
            }

            // 4. Save to Room Database
            dao.insertAll(sentenceEntities)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
