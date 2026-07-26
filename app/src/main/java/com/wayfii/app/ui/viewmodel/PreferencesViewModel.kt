package com.wayfii.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wayfii.app.data.repository.GeocodingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PreferencesViewModel(
    private val geocodingRepository: GeocodingRepository
) : ViewModel() {

    private val _currentLocationName = MutableStateFlow<String?>(null)
    val currentLocationName: StateFlow<String?> = _currentLocationName.asStateFlow()

    private val _isResolvingLocation = MutableStateFlow(false)
    val isResolvingLocation: StateFlow<Boolean> = _isResolvingLocation.asStateFlow()

    fun resolveLocationName(lat: Double, lon: Double) {
        viewModelScope.launch {
            _isResolvingLocation.value = true
            geocodingRepository.reverseGeocode(lat, lon)
                .onSuccess { name ->
                    _currentLocationName.value = name
                }
                .onFailure {
                    _currentLocationName.value = "Mi ubicación"
                }
            _isResolvingLocation.value = false
        }
    }
}
