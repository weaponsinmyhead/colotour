package com.wayfii.app.ui.viewmodel

import com.wayfii.app.data.model.*
import com.wayfii.app.data.repository.ItineraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun `generarAventuras updates uiState to ProposalsLoaded`() = runTest {
        val repository = object : ItineraryRepository {
            override suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary> {
                return Result.success(
                    Itinerary(
                        destino = "Bariloche",
                        actividades = emptyList(),
                        duracionTotal = "3h",
                        costoTotalEstimado = "0",
                        puntoPartida = "Centro",
                        rangoHorarioText = "09:00 a 18:00",
                        incluyeComida = true,
                        cantidadPersonas = 1
                    )
                )
            }
        }
        val viewModel = ItineraryViewModel(repository)
        val prefs = TravelPreferences(
            destino = "Bariloche",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "Centro",
            includeFoodStops = true,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.BAJO,
            ritmo = TravelPace.EQUILIBRADO
        )

        viewModel.generarAventuras(prefs)

        val state = viewModel.uiState.value
        assertTrue(state is ItineraryUiState.ProposalsLoaded)
        if (state is ItineraryUiState.ProposalsLoaded) {
            assertEquals(5, state.proposals.size)
        }
    }
}
