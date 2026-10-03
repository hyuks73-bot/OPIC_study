package com.opic.master.data.local

import androidx.room.*
import com.opic.master.data.model.Sentence
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences ORDER BY orderIndex ASC, id ASC")
    fun getAllSentences(): Flow<List<Sentence>>

    @Query("SELECT * FROM sentences WHERE dayKey = :dayKey ORDER BY orderIndex ASC, id ASC")
    fun getSentencesByDay(dayKey: String): Flow<List<Sentence>>

    @Query("SELECT * FROM sentences WHERE id = :id LIMIT 1")
    suspend fun getSentenceById(id: String): Sentence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sentences: List<Sentence>)

    @Update
    suspend fun update(sentence: Sentence)

    @Query("UPDATE sentences SET localAudioPath = :localPath, isDownloaded = 1 WHERE id = :id")
    suspend fun updateAudioDownloaded(id: String, localPath: String)

    @Query("SELECT COUNT(*) FROM sentences WHERE isDownloaded = 1")
    fun getDownloadedCount(): Flow<Int>
}

@Database(entities = [Sentence::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentenceDao(): SentenceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opic_master_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabaseIfEmpty(context: android.content.Context, dao: SentenceDao) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (dao.getSentenceById("day1_1") == null) {
                    try {
                        val jsonString = context.assets.open("data_manifest.json").bufferedReader().use { it.readText() }
                        val manifest = com.google.gson.Gson().fromJson(jsonString, com.opic.master.data.model.ManifestResponse::class.java)
                        val list = mutableListOf<Sentence>()
                        var orderIndexCounter = 0
                        for ((dayKey, dayData) in manifest.days) {
                            for (item in dayData.sentences) {
                                list.add(
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
                                        isDownloaded = false
                                    )
                                )
                            }
                        }
                        dao.insertAll(list)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
