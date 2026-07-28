package com.wayfii.app.ui.viewmodel

import com.wayfii.app.data.model.*
import com.wayfii.app.data.repository.GamificationRepository
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

    @Test
    fun `completing the final stop records visit and itinerary rewards only once`() = runTest {
        val itineraryRepository = object : ItineraryRepository {
            override suspend fun generarItinerario(
                preferences: TravelPreferences,
            ): Result<Itinerary> = Result.failure(UnsupportedOperationException())
        }
        val gamificationRepository = RecordingGamificationRepository()
        val viewModel = ItineraryViewModel(
            repository = itineraryRepository,
            gamificationRepository = gamificationRepository,
        )
        val start = ItineraryStop(
            order = 1,
            type = StopType.START,
            visualType = ActivityVisualType.START,
            horaInicio = "09:00",
            titulo = "Inicio",
            descripcion = "Punto de partida",
            duracionEstimada = "0m",
            costoEstimado = "Gratuito",
        )
        val museum = ItineraryStop(
            order = 2,
            type = StopType.PLACE,
            visualType = ActivityVisualType.CULTURE,
            horaInicio = "09:15",
            titulo = "Museo",
            descripcion = "Historia local",
            duracionEstimada = "1h",
            costoEstimado = "Gratuito",
            placeId = "place-museum",
        )
        val itinerary = Itinerary(
            destino = "Buenos Aires",
            actividades = listOf(start, museum),
            duracionTotal = "3h",
            costoTotalEstimado = "Gratuito",
            puntoPartida = "Plaza de Mayo",
            rangoHorarioText = "09:00 a 12:00",
            incluyeComida = false,
            cantidadPersonas = 1,
        )
        val proposal = AdventureProposal(
            id = "history-day",
            title = "Historia porteña",
            emoji = "🏛",
            tagline = "Un paseo histórico",
            durationText = "3h",
            distanceText = "2 km",
            difficulty = "Fácil",
            atmosphere = "Cultural",
            highlights = listOf("Museo"),
            mainQuestStops = itinerary.actividades,
            sideQuests = emptyList(),
            baseItinerary = itinerary,
        )

        viewModel.seleccionarAventura(proposal)
        viewModel.toggleCompletarParada(start.order)
        viewModel.toggleCompletarParada(museum.order)

        val state = viewModel.uiState.value as ItineraryUiState.AdventureActive
        assertTrue(state.isFinished)
        assertEquals(setOf(1, 2), state.completedStopOrders)
        assertEquals(60, state.playerProgress.points)
        assertEquals(60, state.rewardFeedback?.awardedPoints)
        assertEquals(1, gamificationRepository.stopCalls)
        assertEquals(1, gamificationRepository.itineraryCalls)

        viewModel.toggleCompletarParada(museum.order)
        assertEquals(1, gamificationRepository.stopCalls)
        assertEquals(1, gamificationRepository.itineraryCalls)
    }

    private class RecordingGamificationRepository : GamificationRepository {
        var stopCalls = 0
        var itineraryCalls = 0
        private var progress = PlayerProgress(
            syncStatus = GamificationSyncStatus.SYNCED,
        )

        override suspend fun loadLocalProgress(): PlayerProgress = progress

        override suspend fun recordStopCompleted(
            adventureSessionId: String,
            destination: String,
            stop: ItineraryStop,
        ): RewardReceipt {
            stopCalls += 1
            progress = progress.copy(points = 20)
            return RewardReceipt(
                recorded = true,
                awardedPoints = 20,
                earnedBadges = listOf("primer_paso"),
                profile = progress,
            )
        }

        override suspend fun recordItineraryCompleted(
            adventureSessionId: String,
            destination: String,
        ): RewardReceipt {
            itineraryCalls += 1
            progress = progress.copy(points = 60)
            return RewardReceipt(
                recorded = true,
                awardedPoints = 40,
                earnedBadges = emptyList(),
                profile = progress,
            )
        }

        override suspend fun syncProgress(): PlayerProgress = progress
    }
}
