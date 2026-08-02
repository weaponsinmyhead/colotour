package com.wayfii.app.domain.engine.context

import com.wayfii.app.data.model.*
import java.util.Calendar

class ContextEngine {

    fun resolveContext(preferences: TravelPreferences): ContextEnvironment {
        // If the user explicitly selected a context override in the UI, use it
        preferences.contextOverride?.let { override ->
            return override.copy(cityName = preferences.destino.ifBlank { "la ciudad" })
        }

        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1 // 1..12
        val dayOfWeekInt = calendar.get(Calendar.DAY_OF_WEEK)
        val dayNames = arrayOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
        val dayOfWeek = dayNames.getOrElse(dayOfWeekInt - 1) { "Sábado" }

        val lat = preferences.lat ?: -34.6037 // Default to Buenos Aires latitude
        val isSouthernHemisphere = lat < 0

        val season = determineSeason(month, isSouthernHemisphere)
        val timeOfDay = determineTimeOfDay(preferences.startMinutes, preferences.endMinutes)
        val (defaultWeather, defaultTemp) = getDefaultWeatherAndTemp(season, timeOfDay)

        val sunrise = if (season == Season.SUMMER) "05:45" else if (season == Season.WINTER) "07:45" else "06:30"
        val sunset = if (season == Season.SUMMER) "20:10" else if (season == Season.WINTER) "18:15" else "19:15"

        return ContextEnvironment(
            season = season,
            timeOfDay = timeOfDay,
            weather = defaultWeather,
            temperatureCelsius = defaultTemp,
            month = month,
            dayOfWeek = dayOfWeek,
            isPublicHoliday = false,
            sunriseTime = sunrise,
            sunsetTime = sunset,
            cityName = preferences.destino.ifBlank { "Buenos Aires" }
        )
    }

    fun determineSeason(month: Int, isSouthernHemisphere: Boolean): Season {
        return if (isSouthernHemisphere) {
            when (month) {
                12, 1, 2 -> Season.SUMMER
                3, 4, 5 -> Season.AUTUMN
                6, 7, 8 -> Season.WINTER
                else -> Season.SPRING
            }
        } else {
            when (month) {
                12, 1, 2 -> Season.WINTER
                3, 4, 5 -> Season.SPRING
                6, 7, 8 -> Season.SUMMER
                else -> Season.AUTUMN
            }
        }
    }

    fun determineTimeOfDay(startMinutes: Int, endMinutes: Int): TimeOfDay {
        val midMinutes = (startMinutes + endMinutes) / 2
        val hour = midMinutes / 60
        return when {
            hour in 6..11 -> TimeOfDay.MORNING
            hour in 12..16 -> TimeOfDay.AFTERNOON
            hour in 17..19 -> TimeOfDay.GOLDEN_HOUR
            else -> TimeOfDay.NIGHT
        }
    }

    private fun getDefaultWeatherAndTemp(season: Season, timeOfDay: TimeOfDay): Pair<WeatherCondition, Int> {
        return when (season) {
            Season.SPRING -> Pair(WeatherCondition.SUNNY, if (timeOfDay == TimeOfDay.NIGHT) 16 else 22)
            Season.SUMMER -> Pair(if (timeOfDay == TimeOfDay.AFTERNOON) WeatherCondition.VERY_HOT else WeatherCondition.SUNNY, if (timeOfDay == TimeOfDay.NIGHT) 22 else 31)
            Season.AUTUMN -> Pair(WeatherCondition.CLOUDY, if (timeOfDay == TimeOfDay.NIGHT) 12 else 18)
            Season.WINTER -> Pair(WeatherCondition.VERY_COLD, if (timeOfDay == TimeOfDay.NIGHT) 7 else 12)
        }
    }
}
