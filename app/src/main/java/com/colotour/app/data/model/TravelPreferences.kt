package com.colotour.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TravelPreferences(
    val destino: String,
    val intereses: Set<TourismInterest>,
    val movilidad: Set<MobilityType>,
    val startMinutes: Int,
    val endMinutes: Int,
    val startingPointName: String,
    val includeFoodStops: Boolean,
    val cantidadPersonas: Int,
    val presupuesto: BudgetLevel,
    val ritmo: TravelPace
)
