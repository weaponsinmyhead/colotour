package com.colotour.app.domain.engine

import com.colotour.app.data.model.BudgetLevel
import com.colotour.app.data.model.Itinerary
import com.colotour.app.data.model.ItineraryStop
import com.colotour.app.data.model.TravelPreferences
import com.colotour.app.data.repository.PlacesRepository
import java.util.Locale

class ItineraryEngine(
    private val placesRepository: PlacesRepository
) {
    private val scorer = PlaceScorer()
    private val routeOptimizer = RouteOptimizer()
    private val timePlanner = TimePlanner()
    private val costEstimator = CostEstimator()

    suspend fun generate(preferences: TravelPreferences): Itinerary {
        // 1. Obtener candidatos locales
        val candidates = placesRepository.getCandidatePlaces(preferences.destino)

        // 2. Puntuar afinidad
        val scored = candidates.map { place ->
            place to scorer.scorePlace(place, preferences)
        }.sortedByDescending { it.second }

        // Tomar hasta los 10 mejores para trazar ruta
        val bestCandidates = scored.take(10).map { it.first }

        // 3. Optimizar ruta (Nearest Neighbor)
        val optimizedRoute = routeOptimizer.optimizeRoute(bestCandidates)

        // 4. Planificar los tiempos
        val plannedActivities = timePlanner.planTimes(
            places = optimizedRoute,
            startMinutes = preferences.startMinutes,
            endMinutes = preferences.endMinutes,
            ritmo = preferences.ritmo
        )

        // 5. Intercalar paradas de comida y punto de partida
        val finalStops = mutableListOf<ItineraryStop>()
        var foodStopsCount = 0

        val startingPoint = preferences.startingPointName.trim()
        if (startingPoint.isNotEmpty()) {
            val startHour = preferences.startMinutes / 60
            val startMin = preferences.startMinutes % 60
            finalStops.add(
                ItineraryStop(
                    horaInicio = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMin),
                    titulo = "Punto de Partida",
                    descripcion = "Inicio del recorrido desde: $startingPoint",
                    duracionEstimada = "0m",
                    costoEstimado = "Gratuito"
                )
            )
        }

        var breakfastAdded = false
        var lunchAdded = false
        var dinnerAdded = false

        for (i in plannedActivities.indices) {
            val activity = plannedActivities[i]
            val currentMinutes = parseTimeToMinutes(activity.horaInicio)

            if (preferences.includeFoodStops) {
                // Desayuno (08:00 - 11:00)
                if (currentMinutes in 480..660 && !breakfastAdded) {
                    finalStops.add(createFoodStop(currentMinutes, "Desayuno / Café", preferences.presupuesto))
                    breakfastAdded = true
                    foodStopsCount++
                }
                // Almuerzo (12:00 - 15:00)
                else if (currentMinutes in 720..900 && !lunchAdded) {
                    finalStops.add(createFoodStop(currentMinutes, "Almuerzo", preferences.presupuesto))
                    lunchAdded = true
                    foodStopsCount++
                }
                // Cena (18:00 - 22:00)
                else if (currentMinutes in 1080..1320 && !dinnerAdded) {
                    finalStops.add(createFoodStop(currentMinutes, "Cena o Merienda", preferences.presupuesto))
                    dinnerAdded = true
                    foodStopsCount++
                }
            }

            // Determinar costo unitario de la actividad
            val costText = if (preferences.presupuesto == BudgetLevel.GRATUITO || activity.place.presupuesto == BudgetLevel.GRATUITO) {
                "Gratuito"
            } else {
                val cost = activity.place.costoBasePorPersona * preferences.cantidadPersonas
                "$${String.format(Locale.getDefault(), "%.0f", cost)} USD"
            }

            finalStops.add(
                ItineraryStop(
                    horaInicio = activity.horaInicio,
                    titulo = activity.place.nombre,
                    descripcion = "${activity.place.descripcion} (Categoría: ${activity.place.estilo.descripcion})",
                    duracionEstimada = activity.duracionEstimada,
                    costoEstimado = costText,
                    latitud = activity.place.latitud,
                    longitud = activity.place.longitud
                )
            )
        }

        // 6. Estimar costos finales del recorrido
        val costResult = costEstimator.estimateCosts(
            activities = plannedActivities,
            cantidadPersonas = preferences.cantidadPersonas,
            movilidadSeleccionada = preferences.movilidad,
            presupuesto = preferences.presupuesto,
            comidasAgregadasCount = foodStopsCount
        )

        val totalCostoText = if (preferences.presupuesto == BudgetLevel.GRATUITO) {
            "Gratuito o gasto opcional"
        } else {
            "Est. $${String.format(Locale.getDefault(), "%.0f", costResult.totalCost)} USD"
        }

        val startHour = preferences.startMinutes / 60
        val startMin = preferences.startMinutes % 60
        val endHour = preferences.endMinutes / 60
        val endMin = preferences.endMinutes % 60
        val rangoHorarioText = String.format(Locale.getDefault(), "%02d:%02d a %02d:%02d", startHour, startMin, endHour, endMin)
        val duracionTotalString = "${(preferences.endMinutes - preferences.startMinutes) / 60} horas disponibles"

        return Itinerary(
            destino = preferences.destino.trim(),
            actividades = finalStops,
            duracionTotal = duracionTotalString,
            costoTotalEstimado = totalCostoText,
            puntoPartida = if (startingPoint.isEmpty()) "Centro de la ciudad" else startingPoint,
            rangoHorarioText = rangoHorarioText,
            incluyeComida = preferences.includeFoodStops,
            cantidadPersonas = preferences.cantidadPersonas
        )
    }

    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    private fun createFoodStop(minutes: Int, tipo: String, presupuesto: BudgetLevel): ItineraryStop {
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

        return ItineraryStop(
            horaInicio = horaInicio,
            titulo = titulo,
            descripcion = descripcion,
            duracionEstimada = "1h 00m",
            costoEstimado = costo
        )
    }
}
