package com.wayfii.app.data.repository

import com.wayfii.app.data.model.GeoPoint

interface GeocodingRepository {
    suspend fun geocode(query: String): Result<GeoPoint>
    suspend fun reverseGeocode(lat: Double, lon: Double): Result<String>
}
