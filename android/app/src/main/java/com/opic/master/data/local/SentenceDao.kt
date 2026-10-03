package com.opic.master.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.opic.master.data.model.DayEntity
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

    @Query("SELECT COUNT(*) FROM sentences WHERE isDownloaded = 1")
    fun getDownloadedCount(): Flow<Int>
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

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "opic_master_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun createDayEntity(dayKey: String, title: String, orderIndex: Int): DayEntity {
            val num = dayKey.filter { it.isDigit() }
            val tabLabel = if (num.isNotEmpty()) "Day $num" else dayKey.uppercase()
            val emoji = when (dayKey) {
                "day1" -> "📋"
                "day2" -> "🌊"
                "day3" -> "🏃"
                "day4" -> "🎸"
                "day5" -> "🚗"
                "day6" -> "🏖️"
                "day7" -> "💼"
                "day8" -> "🍽️"
                "day9" -> "🏥"
                "day10" -> "✈️"
                else -> "📖"
            }
            return DayEntity(
                dayKey = dayKey,
                tabLabel = tabLabel,
                title = title,
                emoji = emoji,
                orderIndex = orderIndex
            )
        }

        suspend fun seedDatabaseIfEmpty(context: android.content.Context, dao: SentenceDao) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                if (dao.getSentenceById("day1_1") == null) {
                    try {
                        val jsonString = context.assets.open("data_manifest.json").bufferedReader().use { it.readText() }
                        val manifest = com.google.gson.Gson().fromJson(jsonString, com.opic.master.data.model.ManifestResponse::class.java)
                        
                        var dayOrderCounter = 0
                        val dayEntities = manifest.days.map { (dayKey, dayData) ->
                            createDayEntity(dayKey, dayData.title, dayOrderCounter++)
                        }
                        dao.insertDays(dayEntities)

                        var orderIndexCounter = 0
                        val list = manifest.days.flatMap { (dayKey, dayData) ->
                            dayData.sentences.map { item ->
                                Sentence(
                                    id = item.id,
                                    dayKey = dayKey,
                                    orderIndex = orderIndexCounter++,
                                    en = item.en,
                                    ko = item.ko,
                                    guide = item.guide,
                                    tip = item.tip,
                                    audioUrl = item.audioUrl,
                                    imageUrl = item.imageUrl
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
