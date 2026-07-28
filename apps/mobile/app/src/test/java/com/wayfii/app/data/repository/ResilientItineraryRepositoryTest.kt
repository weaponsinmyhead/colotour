package com.wayfii.app.data.repository

import com.wayfii.app.data.model.BudgetLevel
import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.MobilityType
import com.wayfii.app.data.model.TourismInterest
import com.wayfii.app.data.model.TravelPace
import com.wayfii.app.data.model.TravelPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResilientItineraryRepositoryTest {

    @Test
    fun `uses the API result without invoking local fallback`() = runTest {
        val remote = FakeItineraryRepository(Result.success(itinerary("API")))
        val local = FakeItineraryRepository(Result.success(itinerary("Local")))
        val repository = ResilientItineraryRepository(remote, local)

        val result = repository.generarItinerario(preferences())

        assertTrue(result.isSuccess)
        assertEquals("API", result.getOrThrow().dataSourceSummary)
        assertEquals(1, remote.calls)
        assertEquals(0, local.calls)
    }

    @Test
    fun `uses local mode when the API is unavailable`() = runTest {
        val remote = FakeItineraryRepository(Result.failure(Exception("API caída")))
        val local = FakeItineraryRepository(Result.success(itinerary("OpenStreetMap local")))
        val repository = ResilientItineraryRepository(remote, local)

        val result = repository.generarItinerario(preferences())

        assertTrue(result.isSuccess)
        assertEquals(
            "Modo local · OpenStreetMap local",
            result.getOrThrow().dataSourceSummary,
        )
        assertEquals(1, remote.calls)
        assertEquals(1, local.calls)
    }

    private class FakeItineraryRepository(
        private val result: Result<Itinerary>,
    ) : ItineraryRepository {
        var calls: Int = 0
            private set

        override suspend fun generarItinerario(
            preferences: TravelPreferences,
        ): Result<Itinerary> {
            calls++
            return result
        }
    }

    private fun preferences() = TravelPreferences(
        destino = "Buenos Aires",
        intereses = setOf(TourismInterest.CULTURAL),
        movilidad = setOf(MobilityType.CAMINANDO),
        startMinutes = 9 * 60,
        endMinutes = 18 * 60,
        startingPointName = "",
        includeFoodStops = true,
        cantidadPersonas = 1,
        presupuesto = BudgetLevel.BAJO,
        ritmo = TravelPace.EQUILIBRADO,
    )

    private fun itinerary(source: String) = Itinerary(
        destino = "Buenos Aires",
        actividades = emptyList(),
        duracionTotal = "9 horas disponibles",
        costoTotalEstimado = "Gratuito",
        puntoPartida = "Centro de la ciudad",
        rangoHorarioText = "09:00 a 18:00",
        incluyeComida = true,
        cantidadPersonas = 1,
        dataSourceSummary = source,
    )
}
