package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AdventureScore(
    val totalScore: Double, // 0.0 .. 100.0
    val seasonRelevance: Double = 15.0,
    val weatherCompatibility: Double = 15.0,
    val goldenHourAlignment: Double = 15.0,
    val walkingComfort: Double = 15.0,
    val storyOriginality: Double = 10.0,
    val poiDiversity: Double = 10.0,
    val photographyPotential: Double = 10.0,
    val contextUniqueness: Double = 10.0,
    val scoreLabel: String = "⚡ ${totalScore.toInt()} pts · Match Contextual"
)
