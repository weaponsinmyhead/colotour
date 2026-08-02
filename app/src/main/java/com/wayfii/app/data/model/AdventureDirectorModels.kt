package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

enum class SurpriseType {
    VIEWPOINT,
    PHOTO_SPOT,
    REST_SPOT,
    CURIOSITY
}

@Serializable
data class DirectorSurpriseMoment(
    val id: String,
    val title: String,
    val narrative: String,
    val type: SurpriseType,
    val iconEmoji: String,
    val actionText: String = "Descubrir",
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Serializable
data class AdventureAdaptation(
    val isAdapted: Boolean = false,
    val adaptationReason: String = "",
    val liveNarrative: String,
    val activeSurpriseMoment: DirectorSurpriseMoment? = null,
    val colorPalette: AdventureColorPalette,
    val adaptedStops: List<ItineraryStop> = emptyList()
)
