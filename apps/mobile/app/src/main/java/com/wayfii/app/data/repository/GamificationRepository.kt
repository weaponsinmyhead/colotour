package com.wayfii.app.data.repository

import com.wayfii.app.data.model.ItineraryStop
import com.wayfii.app.data.model.PlayerProgress
import com.wayfii.app.data.model.RewardReceipt

interface GamificationRepository {
    suspend fun loadLocalProgress(): PlayerProgress

    suspend fun recordStopCompleted(
        adventureSessionId: String,
        destination: String,
        stop: ItineraryStop,
    ): RewardReceipt

    suspend fun recordItineraryCompleted(
        adventureSessionId: String,
        destination: String,
    ): RewardReceipt

    suspend fun syncProgress(): PlayerProgress
}

object NoOpGamificationRepository : GamificationRepository {
    private val emptyProgress = PlayerProgress()

    override suspend fun loadLocalProgress(): PlayerProgress = emptyProgress

    override suspend fun recordStopCompleted(
        adventureSessionId: String,
        destination: String,
        stop: ItineraryStop,
    ): RewardReceipt = RewardReceipt(
        recorded = false,
        awardedPoints = 0,
        earnedBadges = emptyList(),
        profile = emptyProgress,
    )

    override suspend fun recordItineraryCompleted(
        adventureSessionId: String,
        destination: String,
    ): RewardReceipt = RewardReceipt(
        recorded = false,
        awardedPoints = 0,
        earnedBadges = emptyList(),
        profile = emptyProgress,
    )

    override suspend fun syncProgress(): PlayerProgress = emptyProgress
}
