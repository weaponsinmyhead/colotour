package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CityMoment(
    val id: String,
    val name: String,
    val description: String,
    val season: Season? = null,
    val activeMonths: List<Int> = emptyList(), // 1..12
    val activeDaysOfWeek: List<String> = emptyList(), // e.g. "Domingo"
    val isSpecialEvent: Boolean = false,
    val priority: Int = 50, // 1..100
    val contextualReasonLabel: String, // e.g. "🌸 Solo durante la floración de Jacarandás"
    val suggestedThemeIds: List<String> = emptyList(),
    val narrativeTone: String,
    val heroKeywords: List<String> = emptyList(),
    val colorPalette: AdventureColorPalette,
    val recommendedTimeOfDay: TimeOfDay? = null
)
