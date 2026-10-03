package com.opic.master.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "sentences")
data class Sentence(
    @PrimaryKey
    val id: String,
    val dayKey: String,
    val orderIndex: Int = 0,
    val en: String,
    val ko: String,
    val guide: String,
    val tip: String,
    @SerializedName("audio_url")
    val audioUrl: String,
    @SerializedName("image_url")
    val imageUrl: String,
    val localAudioPath: String? = null,
    val localImagePath: String? = null,
    val isDownloaded: Boolean = false,
    val bookmark: Boolean = false,
    val repeatCount: Int = 3
)

@Entity(tableName = "days")
data class DayEntity(
    @PrimaryKey
    val dayKey: String,
    val tabLabel: String,
    val title: String,
    val emoji: String = "📖",
    val orderIndex: Int = 0
)

data class ManifestResponse(
    val version: String,
    val lastUpdated: String,
    val totalSentences: Int,
    val days: Map<String, DayData>
)

data class DayData(
    val title: String,
    val sentences: List<SentenceItem>
)

data class SentenceItem(
    val id: String,
    val en: String,
    val ko: String,
    val guide: String,
    val tip: String,
    @SerializedName("audio_url")
    val audioUrl: String,
    @SerializedName("image_url")
    val imageUrl: String
)
