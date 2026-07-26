package com.wayfii.app.ui.viewmodel

import com.wayfii.app.data.model.*
import com.wayfii.app.data.repository.ItineraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generarItinerario updates uiState to Success`() = runTest {
        val repository = object : ItineraryRepository {
            override suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary> {
                return Result.success(
                    Itinerary(
                        destino = "Test",
                        actividades = emptyList(),
                        duracionTotal = "2h",
                        costoTotalEstimado = "0",
                        puntoPartida = "Test Start",
                        rangoHorarioText = "09:00 a 18:00",
                        incluyeComida = true,
                        cantidadPersonas = 1
                    )
                )
            }
        }
        val viewModel = ItineraryViewModel(repository)
        val prefs = TravelPreferences(
            destino = "Test",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "Test Start",
            includeFoodStops = true,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.BAJO,
            ritmo = TravelPace.EQUILIBRADO
        )

        viewModel.generarItinerario(prefs)

        assertTrue(viewModel.uiState.value is ItineraryUiState.Success)
    }
}
