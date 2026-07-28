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

        // Wikimedia recomienda mantener baja la concurrencia. Se procesan
        // lotes de tres y cada lote termina antes de iniciar el siguiente.
        val urlMap = targetStops.chunked(3).flatMap { batch ->
            batch.map { stop ->
                async {
                    val result = imageRepository.findImageForPlace(
                        stop.titulo,
                        itinerary.destino,
                    )
                    stop.order to result.getOrNull()
                }
            }
                .awaitAll()
        }.toMap()

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
