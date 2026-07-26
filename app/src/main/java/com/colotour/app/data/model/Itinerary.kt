package com.colotour.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ItineraryStop(
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
    val actividades: List<ItineraryStop>,
    val duracionTotal: String,
    val costoTotalEstimado: String,
    val puntoPartida: String,
    val rangoHorarioText: String,
    val incluyeComida: Boolean,
    val cantidadPersonas: Int
)
