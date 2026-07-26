package com.colotour.app.data.repository

import com.colotour.app.data.model.GeoPoint

interface GeocodingRepository {
    suspend fun geocode(query: String): Result<GeoPoint>
}
