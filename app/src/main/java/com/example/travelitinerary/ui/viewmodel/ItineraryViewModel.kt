package com.example.travelitinerary.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelitinerary.data.model.Itinerary
import com.example.travelitinerary.data.model.TravelPreferences
import com.example.travelitinerary.data.repository.ItineraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ItineraryUiState {
    object Idle : ItineraryUiState
    object Loading : ItineraryUiState
    data class Success(val itinerary: Itinerary) : ItineraryUiState
    data class Error(val message: String) : ItineraryUiState
}

class ItineraryViewModel(
    private val repository: ItineraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItineraryUiState>(ItineraryUiState.Idle)
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

    fun generarItinerario(preferences: TravelPreferences) {
        viewModelScope.launch {
            _uiState.value = ItineraryUiState.Loading
            repository.generarItinerario(preferences)
                .onSuccess { itinerary ->
                    _uiState.value = ItineraryUiState.Success(itinerary)
                }
                .onFailure { exception ->
                    _uiState.value = ItineraryUiState.Error(exception.message ?: "Error al generar itinerario")
                }
        }
    }

    fun resetState() {
        _uiState.value = ItineraryUiState.Idle
    }
}
