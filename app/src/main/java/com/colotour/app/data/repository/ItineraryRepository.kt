package com.colotour.app.data.repository

import com.colotour.app.data.model.Itinerary
import com.colotour.app.data.model.TravelPreferences

interface ItineraryRepository {
    suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary>
}
