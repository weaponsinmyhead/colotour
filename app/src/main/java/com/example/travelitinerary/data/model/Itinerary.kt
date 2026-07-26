package com.example.travelitinerary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ItineraryActivity(
    val horaInicio: String,
    val titulo: String,
    val descripcion: String,
    val duracionEstimada: String,
    val costoEstimado: String,
    val latitud: Double? = null,
    val longitud: Double? = null
)

@Serializable
data class Itinerary(
    val destino: String,
    val actividades: List<ItineraryActivity>,
    val duracionTotal: String,
    val costoTotalEstimado: String
)
