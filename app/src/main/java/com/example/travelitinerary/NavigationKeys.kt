package com.example.travelitinerary

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import com.example.travelitinerary.data.model.TravelPreferences

@Serializable
data object Preferences : NavKey

@Serializable
data class ItineraryResult(val preferences: TravelPreferences) : NavKey
