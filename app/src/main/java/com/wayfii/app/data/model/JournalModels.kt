package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PassportStamp(
    val id: String,
    val title: String,
    val emoji: String,
    val themeId: String,
    val unlockedDateText: String,
    val badgeColorHex: String = "#00897B",
    val cityName: String = "Buenos Aires"
)

@Serializable
data class ThematicCollection(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val collectedCount: Int,
    val totalCount: Int,
    val description: String
)

@Serializable
data class JournalMemory(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateTag: String,
    val iconEmoji: String = "✨"
)

@Serializable
data class JournalEntry(
    val id: String,
    val adventureTitle: String,
    val cityName: String,
    val completionDateFormatted: String,
    val heroImageUrl: String,
    val narrative: String,
    val distanceWalkedText: String,
    val durationText: String,
    val estimatedStepsText: String = "4,850 pasos",
    val weatherBadge: String,
    val season: Season,
    val moodTag: String,
    val passportStamp: PassportStamp,
    val mainQuestStops: List<ItineraryStop>,
    val sideQuests: List<SideQuestItem>,
    val userPhotos: List<String> = emptyList(),
    val personalNotes: String = "",
    val ratingStars: Int = 5
)
