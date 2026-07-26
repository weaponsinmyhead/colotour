package com.colotour.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class StopType {
    START,
    PLACE,
    FOOD
}

@Serializable
data class ItineraryStop(
    val order: Int,
    val type: StopType,
    val horaInicio: String,
    val titulo: String,
    val descripcion: String,
    val duracionEstimada: String,
    val costoEstimado: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val reason: String = ""
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
    val cantidadPersonas: Int
)
