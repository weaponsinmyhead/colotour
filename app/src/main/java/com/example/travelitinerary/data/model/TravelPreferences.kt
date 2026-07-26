package com.example.travelitinerary.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TravelPreferences(
    val destino: String,
    val duracion: Duracion,
    val movilidad: Movilidad,
    val cantidadPersonas: Int,
    val presupuesto: Presupuesto,
    val estiloTuristico: EstiloTuristico,
    val ritmo: Ritmo
)
