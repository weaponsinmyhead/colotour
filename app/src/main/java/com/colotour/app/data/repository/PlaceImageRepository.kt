package com.colotour.app.data.repository

interface PlaceImageRepository {
    suspend fun findImageForPlace(placeName: String, destination: String): Result<String?>
}
