package com.colotour.app.domain.engine

import com.colotour.app.data.model.BudgetLevel
import com.colotour.app.data.model.TravelPace
import java.util.Locale

class TimePlanner {
    fun planTimes(
        places: List<CandidatePlace>,
        startMinutes: Int,
        endMinutes: Int,
        ritmo: TravelPace,
        includeFoodStops: Boolean,
        presupuesto: BudgetLevel
    ): List<PlannedStop> {
        val durationMultiplier = when (ritmo) {
            TravelPace.TRANQUILO -> 1.25
            TravelPace.EQUILIBRADO -> 1.0
            TravelPace.INTENSO -> 0.8
        }

        val planned = mutableListOf<PlannedStop>()
        var currentMinutes = startMinutes

        var breakfastAdded = false
        var lunchAdded = false
        var dinnerAdded = false

        var placeIndex = 0

        // Continuamos planificando mientras estemos dentro de la jornada horaria
        while (currentMinutes < endMinutes) {
            // Verificar si corresponde insertar comida antes de planificar un lugar turístico
            if (includeFoodStops) {
                // Desayuno (08:00 - 11:00) -> 480 a 660 min
                if (currentMinutes in 480..600 && !breakfastAdded) {
                    val duration = 45
                    if (currentMinutes + duration <= endMinutes) {
                        planned.add(createFoodPlannedStop(currentMinutes, "Desayuno / Café", duration, presupuesto))
                        currentMinutes += duration
                        breakfastAdded = true
                        continue
                    }
                }
                // Almuerzo (12:00 - 15:00) -> 720 a 900 min
                if (currentMinutes in 720..840 && !lunchAdded) {
                    val duration = 60
                    val transitTime = if (planned.isEmpty()) 0 else 20
                    if (currentMinutes + transitTime + duration <= endMinutes) {
                        currentMinutes += transitTime
                        planned.add(createFoodPlannedStop(currentMinutes, "Almuerzo", duration, presupuesto))
                        currentMinutes += duration
                        lunchAdded = true
                        continue
                    }
                }
                // Cena (18:00 - 22:00) -> 1080 a 1320 min
                if (currentMinutes in 1080..1200 && !dinnerAdded) {
                    val duration = 60
                    val transitTime = if (planned.isEmpty()) 0 else 20
                    if (currentMinutes + transitTime + duration <= endMinutes) {
                        currentMinutes += transitTime
                        planned.add(createFoodPlannedStop(currentMinutes, "Cena o Merienda", duration, presupuesto))
                        currentMinutes += duration
                        dinnerAdded = true
                        continue
                    }
                }
            }

            if (placeIndex >= places.size) {
                break
            }

            // Programar lugar candidato
            val place = places[placeIndex]
            val durationMinutes = (place.duracionRecomendadaMinutos * durationMultiplier).toInt()
            val transitTime = if (planned.isEmpty()) 0 else 20

            if (currentMinutes + transitTime + durationMinutes <= endMinutes) {
                currentMinutes += transitTime
                val startHour = currentMinutes / 60
                val startMin = currentMinutes % 60
                val horaInicioStr = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMin)

                val hours = durationMinutes / 60
                val mins = durationMinutes % 60
                val duracionStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

                planned.add(
                    PlannedStop.PlaceStop(
                        place = place,
                        horaInicio = horaInicioStr,
                        duracionEstimada = duracionStr,
                        durationMinutes = durationMinutes
                    )
                )
                currentMinutes += durationMinutes
                placeIndex++
            } else {
                break
            }
        }

        return planned
    }

    private fun createFoodPlannedStop(minutes: Int, tipo: String, durationMinutes: Int, presupuesto: BudgetLevel): PlannedStop.FoodStop {
        val hour = minutes / 60
        val min = minutes % 60
        val horaInicio = String.format(Locale.getDefault(), "%02d:%02d", hour, min)

        val (titulo, descripcion, costo) = when (presupuesto) {
            BudgetLevel.GRATUITO -> Triple(
                "$tipo al aire libre",
                "Picnic o mercado local al aire libre con opciones gratuitas.",
                "Gratuito o consumo opcional"
            )
            BudgetLevel.BAJO -> Triple(
                "$tipo económico",
                "Establecimiento informal de comida rápida local.",
                "Gasto mínimo"
            )
            BudgetLevel.MEDIO -> Triple(
                "$tipo tradicional",
                "Restaurante típico de menú del día o cafetería céntrica.",
                "Gasto moderado"
            )
            BudgetLevel.ALTO -> Triple(
                "$tipo gourmet",
                "Bistró recomendado para degustar gastronomía de primer nivel.",
                "Gasto alto"
            )
        }

        return PlannedStop.FoodStop(
            horaInicio = horaInicio,
            titulo = titulo,
            descripcion = descripcion,
            duracionEstimada = "${durationMinutes}m",
            costoEstimado = costo,
            durationMinutes = durationMinutes
        )
    }
}

sealed interface PlannedStop {
    val horaInicio: String
    val durationMinutes: Int
    val duracionEstimada: String

    data class PlaceStop(
        val place: CandidatePlace,
        override val horaInicio: String,
        override val duracionEstimada: String,
        override val durationMinutes: Int
    ) : PlannedStop

    data class FoodStop(
        override val horaInicio: String,
        val titulo: String,
        val descripcion: String,
        override val duracionEstimada: String,
        val costoEstimado: String,
        override val durationMinutes: Int
    ) : PlannedStop
}
