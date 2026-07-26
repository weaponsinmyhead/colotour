package com.colotour.app.data.repository

import com.colotour.app.data.model.Itinerary
import com.colotour.app.data.model.TravelPreferences
import com.colotour.app.domain.engine.ItineraryEngine
import kotlinx.coroutines.delay

class MockItineraryRepository : ItineraryRepository {
    private val mockPlacesRepository = MockPlacesRepository()
    private val overpassPlacesRepository = OverpassPlacesRepository()
    private val hybridPlacesRepository = HybridPlacesRepository(overpassPlacesRepository, mockPlacesRepository)
    
    private val geocodingRepository = NominatimGeocodingRepository()
    private val engine = ItineraryEngine(hybridPlacesRepository, geocodingRepository)

    override suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary> {
        delay(1500) // Simula la latencia de la llamada de red/API

        return try {
            if (preferences.destino.isBlank()) {
                Result.failure(IllegalArgumentException("El destino no puede estar vacío"))
            } else {
                val itinerary = engine.generate(preferences)
                Result.success(itinerary)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
