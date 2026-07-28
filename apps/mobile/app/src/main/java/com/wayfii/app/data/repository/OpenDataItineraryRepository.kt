package com.wayfii.app.data.repository

import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.TravelPreferences
import com.wayfii.app.domain.engine.ItineraryEngine
import com.wayfii.app.domain.engine.ItineraryImageEnricher
import java.io.IOException
import kotlinx.coroutines.CancellationException

/**
 * Fallback local basado exclusivamente en fuentes abiertas reales.
 *
 * No completa huecos con lugares, coordenadas ni demoras simuladas. Si una
 * fuente no está disponible, propaga un error para que la UI informe el estado
 * real al viajero.
 */
class OpenDataItineraryRepository(
    overpass: OverpassPlacesRepository = OverpassPlacesRepository(),
    geocoding: GeocodingRepository = NominatimGeocodingRepository(),
    images: PlaceImageRepository = WikimediaPlaceImageRepository(),
) : ItineraryRepository {

    private val places = OpenDataPlacesRepository(overpass)
    private val engine = ItineraryEngine(places, geocoding)
    private val imageEnricher = ItineraryImageEnricher(images)

    override suspend fun generarItinerario(
        preferences: TravelPreferences,
    ): Result<Itinerary> = try {
        require(preferences.destino.isNotBlank()) {
            "El destino no puede estar vacío."
        }
        Result.success(imageEnricher.enrich(engine.generate(preferences)))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

class OpenDataPlacesRepository(
    private val overpass: OverpassPlacesRepository,
) : PlacesRepository, PlacesSourceMetadata {

    override var sourceSummary: String = "OpenStreetMap"
        private set

    override suspend fun getCandidatePlaces(
        destino: String,
        baseLat: Double,
        baseLon: Double,
    ) = overpass.getCandidatePlaces(destino, baseLat, baseLon)
        .getOrElse { cause ->
            throw IOException(
                "No se pudieron consultar lugares reales de OpenStreetMap.",
                cause,
            )
        }
        .also { places ->
            if (places.isEmpty()) {
                throw NoSuchElementException(
                    "No encontramos lugares turísticos reales para este destino.",
                )
            }
            sourceSummary = "Lugares reales de OpenStreetMap"
        }
}
