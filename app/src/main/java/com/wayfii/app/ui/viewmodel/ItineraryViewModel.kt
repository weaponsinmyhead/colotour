package com.wayfii.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayfii.app.data.model.AdventureProposal
import com.wayfii.app.data.model.AdventureProposalGenerator
import com.wayfii.app.data.model.TravelPreferences
import com.wayfii.app.data.repository.ItineraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ItineraryUiState {
    object Idle : ItineraryUiState
    object Loading : ItineraryUiState
    data class ProposalsLoaded(
        val proposals: List<AdventureProposal>,
        val preferences: TravelPreferences
    ) : ItineraryUiState

    data class ProposalDetail(
        val selectedProposal: AdventureProposal,
        val preferences: TravelPreferences,
        val allProposals: List<AdventureProposal>
    ) : ItineraryUiState

    data class AdventureActive(
        val selectedProposal: AdventureProposal,
        val completedStopOrders: Set<Int> = emptySet(),
        val discoveredSideQuestIds: Set<String> = emptySet(),
        val isFinished: Boolean = false
    ) : ItineraryUiState

    data class Error(val message: String) : ItineraryUiState
}

class ItineraryViewModel(
    private val repository: ItineraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItineraryUiState>(ItineraryUiState.Idle)
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

    fun generarAventuras(preferences: TravelPreferences) {
        viewModelScope.launch {
            _uiState.value = ItineraryUiState.Loading
            repository.generarItinerario(preferences)
                .onSuccess { itinerary ->
                    val proposals = AdventureProposalGenerator.generateProposals(itinerary, preferences)
                    // Show Adventure Proposals screen first (discovery mode)
                    _uiState.value = ItineraryUiState.ProposalsLoaded(proposals, preferences)
                }
                .onFailure { exception ->
                    _uiState.value = ItineraryUiState.Error(exception.message ?: "Error al buscar aventuras")
                }
        }
    }

    fun verDetalleAventura(proposal: AdventureProposal) {
        val currentState = _uiState.value
        when (currentState) {
            is ItineraryUiState.ProposalsLoaded -> {
                _uiState.value = ItineraryUiState.ProposalDetail(
                    selectedProposal = proposal,
                    preferences = currentState.preferences,
                    allProposals = currentState.proposals
                )
            }
            is ItineraryUiState.ProposalDetail -> {
                _uiState.value = currentState.copy(selectedProposal = proposal)
            }
            else -> {
                seleccionarAventura(proposal)
            }
        }
    }

    fun seleccionarAventura(proposal: AdventureProposal) {
        verDetalleAventura(proposal)
    }

    fun iniciarAventura(proposal: AdventureProposal) {
        _uiState.value = ItineraryUiState.AdventureActive(selectedProposal = proposal)
    }

    fun volverAPropuestas() {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.ProposalDetail) {
            _uiState.value = ItineraryUiState.ProposalsLoaded(
                proposals = currentState.allProposals,
                preferences = currentState.preferences
            )
        } else {
            _uiState.value = ItineraryUiState.Idle
        }
    }

    fun toggleCompletarParada(order: Int) {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.AdventureActive) {
            val updated = currentState.completedStopOrders.toMutableSet()
            if (updated.contains(order)) {
                updated.remove(order)
            } else {
                updated.add(order)
            }
            val allMainOrders = currentState.selectedProposal.mainQuestStops.map { it.order }.toSet()
            val isAllCompleted = allMainOrders.isNotEmpty() && updated.containsAll(allMainOrders)

            _uiState.value = currentState.copy(
                completedStopOrders = updated,
                isFinished = isAllCompleted
            )
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

    fun finalizarAventura() {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.AdventureActive) {
            _uiState.value = currentState.copy(isFinished = true)
        }
    }

    fun reanudarAventura() {
        val currentState = _uiState.value
        if (currentState is ItineraryUiState.AdventureActive) {
            _uiState.value = currentState.copy(isFinished = false)
        }
    }

    fun resetState() {
        _uiState.value = ItineraryUiState.Idle
    }
}
