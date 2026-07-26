package com.colotour.app.data.repository

import com.colotour.app.domain.engine.CandidatePlace

interface PlacesRepository {
    suspend fun getCandidatePlaces(destino: String): List<CandidatePlace>
}
