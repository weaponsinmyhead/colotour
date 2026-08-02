package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Season(val displayName: String, val emoji: String) {
    SPRING("Primavera", "🌸"),
    SUMMER("Verano", "☀️"),
    AUTUMN("Otoño", "🍂"),
    WINTER("Invierno", "❄️")
}

@Serializable
enum class TimeOfDay(val displayName: String, val emoji: String) {
    MORNING("Mañana", "☕"),
    AFTERNOON("Tarde", "🍃"),
    GOLDEN_HOUR("Hora Dorada", "🌅"),
    NIGHT("Noche", "🌙")
}

@Serializable
enum class WeatherCondition(val displayName: String, val emoji: String) {
    SUNNY("Soleado", "☀️"),
    CLOUDY("Nublado", "☁️"),
    RAIN("Lluvia", "🌧️"),
    VERY_HOT("Caluroso", "🔥"),
    VERY_COLD("Frío Intenso", "🧊")
}

@Serializable
data class ContextEnvironment(
    val season: Season,
    val timeOfDay: TimeOfDay,
    val weather: WeatherCondition,
    val temperatureCelsius: Int = 20,
    val month: Int = 10, // 1..12
    val dayOfWeek: String = "Sábado",
    val isPublicHoliday: Boolean = false,
    val sunriseTime: String = "06:30",
    val sunsetTime: String = "19:00",
    val cityName: String = "Buenos Aires"
) {
    val descriptionBadge: String
        get() = "${season.emoji} ${season.displayName} · ${weather.emoji} ${weather.displayName} (${temperatureCelsius}°C) · ${timeOfDay.emoji} ${timeOfDay.displayName}"
}

@Serializable
data class AdventureColorPalette(
    val primaryHex: String,
    val secondaryHex: String,
    val accentHex: String,
    val cardBgHex: String = "#FFFFFF",
    val darkGradientHex: String = "#000000"
)

@Serializable
data class AdventureDNA(
    val title: String,
    val mood: String,
    val narrative: String,
    val categoryTag: String,
    val heroImageUrl: String,
    val colorPalette: AdventureColorPalette,
    val photographyStyle: String,
    val highlights: List<String>,
    val sideQuestTheme: String,
    val badgeText: String,
    val iconEmoji: String,
    val microcopy: String
)
