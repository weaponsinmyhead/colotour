package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class StopType {
    START,
    PLACE,
    FOOD
}

@Serializable
enum class ActivityVisualType {
    START,
    CULTURE,
    HISTORY,
    FOOD,
    NATURE,
    ADVENTURE,
    SHOPPING,
    PHOTO,
    EVENT,
    FAMILY,
    MAINSTREAM,
    DEFAULT
}

@Serializable
data class ItineraryStop(
    val order: Int,
    val type: StopType,
    val visualType: ActivityVisualType,
    val horaInicio: String,
    val titulo: String,
    val descripcion: String,
    val duracionEstimada: String,
    val costoEstimado: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val reason: String = "",
    val imageUrl: String? = null,
    // Stable catalog identifier returned by the Wayfii API. Local fallback
    // itineraries may not have one, so gamification derives a safe local key.
    val placeId: String? = null
)

@Serializable
data class Itinerary(
    val destino: String,
    val actividades: List<ItineraryStop>,
    val duracionTotal: String,
    val costoTotalEstimado: String,
    val puntoPartida: String,
    val rangoHorarioText: String,
    val incluyeComida: Boolean,
    val cantidadPersonas: Int,
    val isFallbackCoordinates: Boolean = false,
    val dataSourceSummary: String = ""
)
