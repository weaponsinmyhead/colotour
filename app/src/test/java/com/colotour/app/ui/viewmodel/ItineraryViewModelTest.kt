package com.colotour.app.ui.viewmodel

import com.colotour.app.data.model.*
import com.colotour.app.data.repository.ItineraryRepository
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
                        costoTotalEstimado = "0"
                    )
                )
            }
        }
        val viewModel = ItineraryViewModel(repository)
        val prefs = TravelPreferences(
            destino = "Test",
            duracion = Duracion.DURACION_2H,
            movilidad = Movilidad.CAMINANDO,
            cantidadPersonas = 1,
            presupuesto = Presupuesto.BAJO,
            estiloTuristico = EstiloTuristico.CULTURAL,
            ritmo = Ritmo.EQUILIBRADO
        )

        viewModel.generarItinerario(prefs)

        assertTrue(viewModel.uiState.value is ItineraryUiState.Success)
    }
}
