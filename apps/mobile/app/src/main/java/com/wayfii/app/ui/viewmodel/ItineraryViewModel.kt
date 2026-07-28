package com.wayfii.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayfii.app.data.model.AdventureProposal
import com.wayfii.app.data.model.AdventureProposalGenerator
import com.wayfii.app.data.model.PlayerProgress
import com.wayfii.app.data.model.RewardReceipt
import com.wayfii.app.data.model.StopType
import com.wayfii.app.data.model.TravelPreferences
import com.wayfii.app.data.repository.GamificationRepository
import com.wayfii.app.data.repository.ItineraryRepository
import com.wayfii.app.data.repository.NoOpGamificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface ItineraryUiState {
    object Idle : ItineraryUiState
    object Loading : ItineraryUiState
    data class ProposalsLoaded(
        val proposals: List<AdventureProposal>,
        val preferences: TravelPreferences
    ) : ItineraryUiState

    data class AdventureActive(
        val selectedProposal: AdventureProposal,
        val adventureSessionId: String,
        val completedStopOrders: Set<Int> = emptySet(),
        val discoveredSideQuestIds: Set<String> = emptySet(),
        val playerProgress: PlayerProgress = PlayerProgress(),
        val isProgressLoading: Boolean = true,
        val isFinished: Boolean = false,
        val rewardFeedback: RewardFeedback? = null,
        val progressError: String? = null,
    ) : ItineraryUiState

    data class Error(val message: String) : ItineraryUiState
}

data class RewardFeedback(
    val id: Long,
    val awardedPoints: Int,
    val earnedBadges: List<String>,
    val message: String,
)

class ItineraryViewModel(
    private val repository: ItineraryRepository,
    private val gamificationRepository: GamificationRepository = NoOpGamificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItineraryUiState>(ItineraryUiState.Idle)
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()
    private val gamificationSyncMutex = Mutex()
    private var feedbackSequence = 0L

    fun generarAventuras(preferences: TravelPreferences) {
        viewModelScope.launch {
            _uiState.value = ItineraryUiState.Loading
            repository.generarItinerario(preferences)
                .onSuccess { itinerary ->
                    val proposals = AdventureProposalGenerator.generateProposals(itinerary, preferences)
                    _uiState.value = ItineraryUiState.ProposalsLoaded(proposals, preferences)
                }
                .onFailure { exception ->
                    _uiState.value = ItineraryUiState.Error(exception.message ?: "Error al buscar aventuras")
                }
        }
    }

    fun seleccionarAventura(proposal: AdventureProposal) {
        val sessionId = buildAdventureSessionId(proposal)
        _uiState.value = ItineraryUiState.AdventureActive(
            selectedProposal = proposal,
            adventureSessionId = sessionId,
        )
        viewModelScope.launch {
            runCatching { gamificationRepository.loadLocalProgress() }
                .onSuccess { progress ->
                    updateAdventure(sessionId) { current ->
                        current.copy(
                            playerProgress = progress,
                            isProgressLoading = false,
                            progressError = null,
                        )
                    }
                }
                .onFailure { exception ->
                    updateAdventure(sessionId) { current ->
                        current.copy(
                            isProgressLoading = false,
                            progressError = exception.message
                                ?: "No se pudo leer el progreso guardado.",
                        )
                    }
                }
            syncGamification(sessionId)
        }
    }

    fun toggleCompletarParada(order: Int) {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.AdventureActive) {
            if (order in currentState.completedStopOrders) return
            val stop = currentState.selectedProposal.mainQuestStops
                .firstOrNull { it.order == order }
                ?: return
            val updated = currentState.completedStopOrders + order
            val allMainOrders = currentState.selectedProposal.mainQuestStops.map { it.order }.toSet()
            val completedAdventure = allMainOrders.isNotEmpty() && updated.containsAll(allMainOrders)

            _uiState.value = currentState.copy(
                completedStopOrders = updated,
            )

            viewModelScope.launch {
                val receipts = mutableListOf<RewardReceipt>()
                var rewardError: Throwable? = null

                if (stop.type != StopType.START) {
                    runCatching {
                        gamificationRepository.recordStopCompleted(
                            adventureSessionId = currentState.adventureSessionId,
                            destination = currentState.selectedProposal.baseItinerary.destino,
                            stop = stop,
                        )
                    }.onSuccess { receipts += it }
                        .onFailure { rewardError = it }
                }

                if (completedAdventure) {
                    runCatching {
                        gamificationRepository.recordItineraryCompleted(
                            adventureSessionId = currentState.adventureSessionId,
                            destination = currentState.selectedProposal.baseItinerary.destino,
                        )
                    }.onSuccess { receipts += it }
                        .onFailure { if (rewardError == null) rewardError = it }
                }

                val awardedPoints = receipts.sumOf { it.awardedPoints }
                val earnedBadges = receipts.flatMap { it.earnedBadges }.distinct()
                val latestProfile = receipts.lastOrNull()?.profile
                val feedback = rewardFeedback(
                    awardedPoints = awardedPoints,
                    earnedBadges = earnedBadges,
                    completedAdventure = completedAdventure,
                    rewardError = rewardError,
                )

                updateAdventure(currentState.adventureSessionId) { current ->
                    current.copy(
                        playerProgress = latestProfile ?: current.playerProgress,
                        isProgressLoading = false,
                        isFinished = completedAdventure,
                        rewardFeedback = feedback,
                        progressError = rewardError?.message,
                    )
                }
                syncGamification(currentState.adventureSessionId)
            }
        }
    }

    fun toggleDescubrirSideQuest(sideQuestId: String) {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.AdventureActive) {
            val updated = currentState.discoveredSideQuestIds.toMutableSet()
            if (updated.contains(sideQuestId)) {
                updated.remove(sideQuestId)
            } else {
                updated.add(sideQuestId)
            }
            _uiState.value = currentState.copy(discoveredSideQuestIds = updated)
        }
    }

    fun clearRewardFeedback() {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.AdventureActive) {
            _uiState.value = currentState.copy(rewardFeedback = null)
        }
    }

    fun resetState() {
        _uiState.value = ItineraryUiState.Idle
    }

    private suspend fun syncGamification(sessionId: String) {
        gamificationSyncMutex.withLock {
            runCatching { gamificationRepository.syncProgress() }
                .onSuccess { progress ->
                    updateAdventure(sessionId) { current ->
                        current.copy(
                            playerProgress = progress,
                            isProgressLoading = false,
                            progressError = null,
                        )
                    }
                }
                .onFailure { exception ->
                    updateAdventure(sessionId) { current ->
                        current.copy(
                            isProgressLoading = false,
                            progressError = exception.message
                                ?: "El progreso quedó pendiente de sincronización.",
                        )
                    }
                }
        }
    }

    private fun updateAdventure(
        sessionId: String,
        update: (ItineraryUiState.AdventureActive) -> ItineraryUiState.AdventureActive,
    ) {
        val latest = _uiState.value
        if (
            latest is ItineraryUiState.AdventureActive &&
            latest.adventureSessionId == sessionId
        ) {
            _uiState.value = update(latest)
        }
    }

    private fun rewardFeedback(
        awardedPoints: Int,
        earnedBadges: List<String>,
        completedAdventure: Boolean,
        rewardError: Throwable?,
    ): RewardFeedback? {
        val message = when {
            rewardError != null ->
                "Completado. La recompensa quedó pendiente de guardado."
            awardedPoints > 0 && completedAdventure ->
                "¡Recorrido completado! +$awardedPoints puntos"
            awardedPoints > 0 ->
                "Parada completada · +$awardedPoints puntos"
            completedAdventure ->
                "Este recorrido ya había sido recompensado."
            else ->
                null
        } ?: return null

        feedbackSequence += 1
        return RewardFeedback(
            id = feedbackSequence,
            awardedPoints = awardedPoints,
            earnedBadges = earnedBadges,
            message = message,
        )
    }

    private fun buildAdventureSessionId(proposal: AdventureProposal): String = listOf(
        proposal.id,
        proposal.baseItinerary.destino.trim(),
        proposal.baseItinerary.rangoHorarioText.trim(),
    ).joinToString(separator = "|")
}
