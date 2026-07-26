package com.colotour.app.domain.engine

import com.colotour.app.data.model.TourismInterest
import com.colotour.app.data.model.BudgetLevel

data class CandidatePlace(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val latitud: Double,
    val longitud: Double,
    val estilo: TourismInterest,
    val presupuesto: BudgetLevel,
    val duracionRecomendadaMinutos: Int,
    val costoBasePorPersona: Double,
    val popularidad: Double
)
