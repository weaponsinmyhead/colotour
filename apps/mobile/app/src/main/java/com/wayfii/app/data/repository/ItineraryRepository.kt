package com.wayfii.app.data.repository

import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.TravelPreferences

interface ItineraryRepository {
    suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary>
}
