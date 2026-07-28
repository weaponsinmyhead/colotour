package com.wayfii.app.domain.engine

import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.StopType
import com.wayfii.app.data.repository.PlaceImageRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ItineraryImageEnricher(private val imageRepository: PlaceImageRepository) {

    suspend fun enrich(itinerary: Itinerary): Itinerary = coroutineScope {
        // Limitar a máximo 6 lugares turísticos para optimizar red
        val placeStops = itinerary.actividades.filter { it.type == StopType.PLACE }
        val targetStops = placeStops.take(6)

        val deferredUrls = targetStops.map { stop ->
            async {
                val result = imageRepository.findImageForPlace(stop.titulo, itinerary.destino)
                stop.order to result.getOrNull()
            }
        }

        val urlMap = deferredUrls.awaitAll().toMap()

        val enrichedStops = itinerary.actividades.map { stop ->
            if (urlMap.containsKey(stop.order)) {
                stop.copy(imageUrl = urlMap[stop.order])
            } else {
                stop
            }
        }

        itinerary.copy(actividades = enrichedStops)
    }
}
