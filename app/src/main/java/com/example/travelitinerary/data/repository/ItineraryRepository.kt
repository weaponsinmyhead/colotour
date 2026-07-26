package com.example.travelitinerary.data.repository

import com.example.travelitinerary.data.model.Itinerary
import com.example.travelitinerary.data.model.TravelPreferences

interface ItineraryRepository {
    suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary>
}
