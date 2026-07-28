package com.wayfii.app.domain.engine

import com.wayfii.app.data.model.TourismInterest
import com.wayfii.app.data.model.BudgetLevel

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
    val popularidad: Double,
    val imageUrl: String? = null
)
