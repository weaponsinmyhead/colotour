package com.wayfii.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GamificationActivityType(
    val points: Int,
) {
    @SerialName("place_visited")
    PLACE_VISITED(20),

    @SerialName("event_attended")
    EVENT_ATTENDED(30),

    @SerialName("itinerary_completed")
    ITINERARY_COMPLETED(40),
}

@Serializable
data class GamificationActivity(
    val idempotencyKey: String,
    val type: GamificationActivityType,
    val subjectId: String,
    val occurredAtEpochMillis: Long,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class GamificationSyncStatus {
    SYNCED,
    PENDING,
    LOCAL_ONLY,
}

@Serializable
data class PlayerProgress(
    val points: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val badges: List<String> = emptyList(),
    val lastActivityAtEpochMillis: Long = 0,
    val pendingSyncCount: Int = 0,
    val syncStatus: GamificationSyncStatus = GamificationSyncStatus.LOCAL_ONLY,
)

data class RewardReceipt(
    val recorded: Boolean,
    val awardedPoints: Int,
    val earnedBadges: List<String>,
    val profile: PlayerProgress,
)
