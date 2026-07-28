package com.wayfii.app.data.repository

import com.wayfii.app.domain.engine.CandidatePlace

interface PlacesRepository {
    suspend fun getCandidatePlaces(destino: String, baseLat: Double, baseLon: Double): List<CandidatePlace>
}
