package com.colotour.app.domain.engine

import com.colotour.app.data.model.TravelPace
import java.util.Locale

class TimePlanner {
    fun planTimes(
        places: List<CandidatePlace>,
        startMinutes: Int,
        endMinutes: Int,
        ritmo: TravelPace
    ): List<PlannedActivity> {
        val durationMultiplier = when (ritmo) {
            TravelPace.TRANQUILO -> 1.25
            TravelPace.EQUILIBRADO -> 1.0
            TravelPace.INTENSO -> 0.8
        }

        val planned = mutableListOf<PlannedActivity>()
        var currentMinutes = startMinutes

        for (i in places.indices) {
            val place = places[i]
            val durationMinutes = (place.duracionRecomendadaMinutos * durationMultiplier).toInt()
            val transitTime = if (i == 0) 0 else 20

            // Detener si excede la hora de cierre seleccionada
            if (i > 0 && (currentMinutes + transitTime + durationMinutes) > endMinutes) {
                break
            }

            currentMinutes += transitTime

            val startHour = currentMinutes / 60
            val startMin = currentMinutes % 60
            val horaInicioStr = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMin)

            val hours = durationMinutes / 60
            val mins = durationMinutes % 60
            val duracionStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            planned.add(
                PlannedActivity(
                    place = place,
                    horaInicio = horaInicioStr,
                    duracionEstimada = duracionStr,
                    durationMinutes = durationMinutes
                )
            )

            currentMinutes += durationMinutes
        }

        return planned
    }
}

data class PlannedActivity(
    val place: CandidatePlace,
    val horaInicio: String,
    val duracionEstimada: String,
    val durationMinutes: Int
)
