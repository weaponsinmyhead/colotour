package com.colotour.app.domain.engine

import com.colotour.app.data.model.*
import com.colotour.app.data.repository.PlaceImageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItineraryImageEnricherTest {

    private class FakePlaceImageRepository : PlaceImageRepository {
        var shouldFail = false
        val calls = mutableListOf<String>()
        val results = mapOf(
            "obelisco buenos aires" to "https://image.com/obelisco.jpg",
            "parque centenario buenos aires" to "https://image.com/parque.jpg"
        )

        override suspend fun findImageForPlace(placeName: String, destination: String): Result<String?> {
            val key = "$placeName $destination".trim().lowercase()
            calls.add(key)
            if (shouldFail) {
                return Result.failure(Exception("Red caida"))
            }
            return Result.success(results[key])
        }
    }

    @Test
    fun `test imagen encontrada y asignada a paradas`() = runTest {
        val fakeRepo = FakePlaceImageRepository()
        val enricher = ItineraryImageEnricher(fakeRepo)

        val stops = listOf(
            ItineraryStop(
                order = 1,
                type = StopType.START,
                visualType = ActivityVisualType.START,
                horaInicio = "09:00",
                titulo = "Inicio",
                descripcion = "Inicio del recorrido",
                duracionEstimada = "0m",
                costoEstimado = "Gratuito"
            ),
            ItineraryStop(
                order = 2,
                type = StopType.PLACE,
                visualType = ActivityVisualType.DEFAULT,
                horaInicio = "10:00",
                titulo = "Obelisco",
                descripcion = "Monumento histórico",
                duracionEstimada = "60m",
                costoEstimado = "Gratuito"
            )
        )
        val itinerary = Itinerary(
            destino = "Buenos Aires",
            actividades = stops,
            duracionTotal = "1 hora",
            costoTotalEstimado = "Gratuito",
            puntoPartida = "Inicio",
            rangoHorarioText = "09:00 a 10:00",
            incluyeComida = false,
            cantidadPersonas = 1
        )

        val result = enricher.enrich(itinerary)

        assertEquals("https://image.com/obelisco.jpg", result.actividades[1].imageUrl)
        assertNull(result.actividades[0].imageUrl) // El punto de partida queda nulo
    }

    @Test
    fun `test imagen no encontrada mantiene imageUrl en null`() = runTest {
        val fakeRepo = FakePlaceImageRepository()
        val enricher = ItineraryImageEnricher(fakeRepo)

        val stops = listOf(
            ItineraryStop(
                order = 2,
                type = StopType.PLACE,
                visualType = ActivityVisualType.DEFAULT,
                horaInicio = "10:00",
                titulo = "Plaza del Carmen",
                descripcion = "Plaza tradicional",
                duracionEstimada = "60m",
                costoEstimado = "Gratuito"
            )
        )
        val itinerary = Itinerary(
            destino = "Buenos Aires",
            actividades = stops,
            duracionTotal = "1 hora",
            costoTotalEstimado = "Gratuito",
            puntoPartida = "Inicio",
            rangoHorarioText = "09:00 a 10:00",
            incluyeComida = false,
            cantidadPersonas = 1
        )

        val result = enricher.enrich(itinerary)
        assertNull(result.actividades[0].imageUrl)
    }

    @Test
    fun `test error de red en repositorio no rompe enriquecimiento`() = runTest {
        val fakeRepo = FakePlaceImageRepository().apply { shouldFail = true }
        val enricher = ItineraryImageEnricher(fakeRepo)

        val stops = listOf(
            ItineraryStop(
                order = 2,
                type = StopType.PLACE,
                visualType = ActivityVisualType.DEFAULT,
                horaInicio = "10:00",
                titulo = "Obelisco",
                descripcion = "Monumento histórico",
                duracionEstimada = "60m",
                costoEstimado = "Gratuito"
            )
        )
        val itinerary = Itinerary(
            destino = "Buenos Aires",
            actividades = stops,
            duracionTotal = "1 hora",
            costoTotalEstimado = "Gratuito",
            puntoPartida = "Inicio",
            rangoHorarioText = "09:00 a 10:00",
            incluyeComida = false,
            cantidadPersonas = 1
        )

        val result = enricher.enrich(itinerary)
        assertNull(result.actividades[0].imageUrl)
    }
}
