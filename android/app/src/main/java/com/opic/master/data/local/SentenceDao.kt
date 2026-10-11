package com.opic.master.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.opic.master.data.model.DayEntity
import com.opic.master.data.model.ManifestResponse
import com.opic.master.data.model.Sentence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences ORDER BY orderIndex ASC, id ASC")
    fun getAllSentences(): Flow<List<Sentence>>

    @Query("SELECT * FROM sentences WHERE id = :id LIMIT 1")
    suspend fun getSentenceById(id: String): Sentence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sentences: List<Sentence>)

    @Query("UPDATE sentences SET localAudioPath = :localPath, isDownloaded = 1 WHERE id = :id")
    suspend fun updateAudioDownloaded(id: String, localPath: String)

    @Query("UPDATE sentences SET localImagePath = :localPath WHERE id = :id")
    suspend fun updateImageDownloaded(id: String, localPath: String)

    @Query("SELECT * FROM days ORDER BY orderIndex ASC")
    fun getAllDays(): Flow<List<DayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDays(days: List<DayEntity>)

    @Query("DELETE FROM days WHERE dayKey NOT IN (:activeDayKeys)")
    suspend fun deleteDaysNotIn(activeDayKeys: List<String>)

    @Query("DELETE FROM sentences WHERE dayKey NOT IN (:activeDayKeys)")
    suspend fun deleteSentencesNotInDays(activeDayKeys: List<String>)

    @Query("DELETE FROM sentences WHERE id NOT IN (:activeSentenceIds)")
    suspend fun deleteSentencesNotIn(activeSentenceIds: List<String>)

    @Query("SELECT * FROM sentences")
    suspend fun getAllSentencesList(): List<Sentence>
}

@Database(entities = [Sentence::class, DayEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentenceDao(): SentenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe migration preserving user data
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Safe migration preserving user data
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opic_master_database",
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun createDayEntity(dayKey: String, title: String, orderIndex: Int): DayEntity {
            val tabLabel = when (dayKey) {
                "transitions" -> "만능 전환어"
                "day1" -> "Day 1"
                "day2-1" -> "Day 2-1"
                "day2-2" -> "Day 2-2"
                "day3-1" -> "Day 3-1"
                "day3-2" -> "Day 3-2"
                "day4-1" -> "Day 4-1"
                "day4-2" -> "Day 4-2"
                "day5-1" -> "Day 5-1"
                "day5-2" -> "Day 5-2"
                "day6" -> "Day 6"
                "day7-1" -> "Day 7-1"
                "day7-2" -> "Day 7-2"
                else -> {
                    val num = dayKey.filter { it.isDigit() || it == '-' }
                    if (num.isNotEmpty()) "Day $num" else dayKey.uppercase()
                }
            }
            val emoji = when (dayKey) {
                "transitions" -> "✨"
                "day1" -> "📋"
                "day2-1" -> "🌊"
                "day2-2" -> "🛏️"
                "day3-1" -> "🏃‍♂️"
                "day3-2" -> "🎵"
                "day4-1" -> "🏃"
                "day4-2" -> "🎸"
                "day5-1" -> "♨️"
                "day5-2" -> "🚗"
                "day6" -> "🏖️"
                "day7-1" -> "📞"
                "day7-2" -> "⚠️"
                "day2" -> "🌊"
                "day3" -> "🏃"
                "day4" -> "🎸"
                "day5" -> "🚗"
                "day7" -> "🎭"
                else -> "📖"
            }
            return DayEntity(
                dayKey = dayKey,
                tabLabel = tabLabel,
                title = title,
                emoji = emoji,
                orderIndex = orderIndex,
            )
        }

        suspend fun seedDatabaseIfEmpty(context: Context, dao: SentenceDao) {
            withContext(Dispatchers.IO) {
                val needsSeed = dao.getSentenceById("day1_1") == null ||
                        dao.getSentenceById("tr_1") == null ||
                        dao.getSentenceById("park_1")?.dayKey != "day2-1" ||
                        dao.getSentenceById("day7_rp1_3")?.en?.contains("Around 9 or 10") != true
                if (needsSeed) {
                    try {
                        val jsonString = context.assets.open("data_manifest.json").bufferedReader().use { it.readText() }
                        val manifest = Gson().fromJson(jsonString, ManifestResponse::class.java)

                        val activeDayKeys = manifest.days.keys.toList()
                        var dayOrderCounter = 0
                        val dayEntities = manifest.days.map { (dayKey, dayData) ->
                            createDayEntity(dayKey, dayData.title, dayOrderCounter++)
                        }
                        dao.insertDays(dayEntities)
                        if (activeDayKeys.isNotEmpty()) {
                            dao.deleteDaysNotIn(activeDayKeys)
                            dao.deleteSentencesNotInDays(activeDayKeys)
                        }

                        val audioDir = java.io.File(context.filesDir, "audio").apply {
                            if (!exists()) mkdirs()
                        }
                        val assetAudioList = try {
                            context.assets.list("audio")?.toSet() ?: emptySet()
                        } catch (_: Exception) {
                            emptySet()
                        }

                        // Copy bundled updated audio assets to internal storage
                        for (item in manifest.days.values.flatMap { it.sentences }) {
                            if (assetAudioList.contains("${item.id}.mp3")) {
                                val destFile = java.io.File(audioDir, "${item.id}.mp3")
                                try {
                                    context.assets.open("audio/${item.id}.mp3").use { input ->
                                        java.io.FileOutputStream(destFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                            }
                        }

                        val existingSentences = dao.getAllSentencesList().associateBy { it.id }
                        var orderIndexCounter = 0
                        val list = manifest.days.flatMap { (dayKey, dayData) ->
                            dayData.sentences.map { item ->
                                val existing = existingSentences[item.id]
                                val localAudioFile = java.io.File(audioDir, "${item.id}.mp3")
                                val hasLocalAudio = localAudioFile.exists() && localAudioFile.length() > 0L
                                val localAudioPath = if (hasLocalAudio) localAudioFile.absolutePath else existing?.localAudioPath
                                val isDownloaded = hasLocalAudio || existing?.isDownloaded == true
                                Sentence(
                                    id = item.id,
                                    dayKey = dayKey,
                                    orderIndex = orderIndexCounter++,
                                    en = item.en,
                                    ko = item.ko,
                                    guide = item.guide,
                                    tip = item.tip,
                                    audioUrl = item.audioUrl,
                                    imageUrl = item.imageUrl,
                                    localAudioPath = localAudioPath,
                                    localImagePath = existing?.localImagePath,
                                    isDownloaded = isDownloaded,
                                    bookmark = existing?.bookmark ?: false,
                                    repeatCount = existing?.repeatCount ?: 3,
                                )
                            }
                        }
                        dao.insertAll(list)
                        val activeSentenceIds = list.map { it.id }
                        if (activeSentenceIds.isNotEmpty()) {
                            dao.deleteSentencesNotIn(activeSentenceIds)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
