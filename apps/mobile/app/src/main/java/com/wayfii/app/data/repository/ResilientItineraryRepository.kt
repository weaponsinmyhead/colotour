package com.wayfii.app.data.repository

import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.TravelPreferences
import kotlinx.coroutines.CancellationException

class ResilientItineraryRepository(
    private val primary: ItineraryRepository?,
    private val localFallback: ItineraryRepository,
) : ItineraryRepository {

    override suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary> {
        val primaryResult = primary?.safeGenerate(preferences)
        if (primaryResult?.isSuccess == true) {
            return primaryResult
        }

        val fallbackResult = localFallback.safeGenerate(preferences)
        if (fallbackResult.isSuccess) {
            return fallbackResult.map { itinerary ->
                itinerary.copy(
                    dataSourceSummary = "Modo local · ${itinerary.dataSourceSummary}",
                )
            }
        }

        val fallbackError = fallbackResult.exceptionOrNull()
            ?: IllegalStateException("No se pudo generar el itinerario.")
        primaryResult?.exceptionOrNull()?.let(fallbackError::addSuppressed)
        return Result.failure(fallbackError)
    }

    private suspend fun ItineraryRepository.safeGenerate(
        preferences: TravelPreferences,
    ): Result<Itinerary> = try {
        generarItinerario(preferences)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}
