package com.colotour.app.domain.engine

import com.colotour.app.data.model.*
import com.colotour.app.data.repository.PlacesRepository
import java.util.Locale
import kotlin.math.abs

class ItineraryEngine(
    private val placesRepository: PlacesRepository
) {
    private val scorer = PlaceScorer()
    private val routeOptimizer = RouteOptimizer()
    private val timePlanner = TimePlanner()
    private val costEstimator = CostEstimator()

    suspend fun generate(preferences: TravelPreferences): Itinerary {
        // 1. Resolver punto de partida y coordenadas mock del destino
        val hash = preferences.destino.lowercase().hashCode()
        val baseLat = -34.6037 + (abs(hash) % 1000) * 0.0001
        val baseLon = -58.3816 + ((abs(hash) / 1000) % 1000) * 0.0001

        val startingPoint = preferences.startingPointName.trim()
        val startPointName = if (startingPoint.isEmpty()) "Centro de la ciudad" else startingPoint
        val startPoint = StartPoint(name = startPointName, latitude = baseLat, longitude = baseLon)

        // 2. Obtener candidatos locales
        val candidates = placesRepository.getCandidatePlaces(preferences.destino)

        // 3. Seleccionar los mejores candidatos incentivando la diversidad de intereses
        val selectedCandidates = mutableListOf<CandidatePlace>()
        val availableCandidates = candidates.toMutableList()
        val seenInterests = mutableSetOf<TourismInterest>()

        for (step in 1..10) {
            if (availableCandidates.isEmpty()) break
            val best = availableCandidates.maxByOrNull { scorer.scorePlace(it, preferences, seenInterests) } ?: break
            selectedCandidates.add(best)
            availableCandidates.remove(best)
            seenInterests.add(best.estilo)
        }

        // 4. Resolver orden del recorrido desde el punto de partida (Nearest Neighbor)
        val optimizedRoute = routeOptimizer.optimizeRoute(startPoint, selectedCandidates)

        // 5. Planificar tiempos (sin solapamiento, con comidas integradas)
        val plannedStops = timePlanner.planTimes(
            places = optimizedRoute,
            startMinutes = preferences.startMinutes,
            endMinutes = preferences.endMinutes,
            ritmo = preferences.ritmo,
            includeFoodStops = preferences.includeFoodStops,
            presupuesto = preferences.presupuesto
        )

        // 6. Estimar costos finales
        val costResult = costEstimator.estimateCosts(
            activities = plannedStops,
            cantidadPersonas = preferences.cantidadPersonas,
            movilidadSeleccionada = preferences.movilidad,
            presupuesto = preferences.presupuesto
        )

        // 7. Mapear a modelos públicos agregando orden, tipo y razones descriptivas
        val finalStops = mutableListOf<ItineraryStop>()
        
        // Agregar parada inicial
        val startHour = preferences.startMinutes / 60
        val startMin = preferences.startMinutes % 60
        finalStops.add(
            ItineraryStop(
                order = 1,
                type = StopType.START,
                visualType = ActivityVisualType.START,
                horaInicio = String.format(Locale.getDefault(), "%02d:%02d", startHour, startMin),
                titulo = "Inicio: $startPointName",
                descripcion = "Comienzo del recorrido de exploración.",
                duracionEstimada = "0m",
                costoEstimado = "Gratuito",
                latitud = startPoint.latitude,
                longitud = startPoint.longitude,
                reason = "Punto de partida configurado."
            )
        )

        // Mapear paradas planificadas
        plannedStops.forEachIndexed { index, planned ->
            val orderNumber = index + 2
            when (planned) {
                is PlannedStop.PlaceStop -> {
                    val place = planned.place
                    val costForActivity = costResult.activityCosts[place.id] ?: 0.0
                    val costText = if (preferences.presupuesto == BudgetLevel.GRATUITO || place.presupuesto == BudgetLevel.GRATUITO) {
                        "Gratuito"
                    } else {
                        "$${String.format(Locale.getDefault(), "%.0f", costForActivity)} USD"
                    }

                    val reason = if (preferences.intereses.contains(place.estilo)) {
                        "Elegido por tu interés en ${place.estilo.descripcion}."
                    } else {
                        "Sugerido por alta afinidad y cercanía en la ruta."
                    }

                    finalStops.add(
                        ItineraryStop(
                            order = orderNumber,
                            type = StopType.PLACE,
                            visualType = mapEstiloToVisualType(place.estilo),
                            horaInicio = planned.horaInicio,
                            titulo = place.nombre,
                            descripcion = "${place.descripcion} (Categoría: ${place.estilo.descripcion})",
                            duracionEstimada = planned.duracionEstimada,
                            costoEstimado = costText,
                            latitud = place.latitud,
                            longitud = place.longitud,
                            reason = reason
                        )
                    )
                }
                is PlannedStop.FoodStop -> {
                    finalStops.add(
                        ItineraryStop(
                            order = orderNumber,
                            type = StopType.FOOD,
                            visualType = ActivityVisualType.FOOD,
                            horaInicio = planned.horaInicio,
                            titulo = planned.titulo,
                            descripcion = planned.descripcion,
                            duracionEstimada = planned.duracionEstimada,
                            costoEstimado = planned.costoEstimado,
                            latitud = null,
                            longitud = null,
                            reason = "Programado según tu horario de viaje y presupuesto."
                        )
                    )
                }
            }
        }

        val totalCostoText = if (preferences.presupuesto == BudgetLevel.GRATUITO) {
            "Gratuito o gasto opcional"
        } else {
            "Est. $${String.format(Locale.getDefault(), "%.0f", costResult.totalCost)} USD"
        }

        val endHour = preferences.endMinutes / 60
        val endMin = preferences.endMinutes % 60
        val rangoHorarioText = String.format(Locale.getDefault(), "%02d:%02d a %02d:%02d", startHour, startMin, endHour, endMin)
        val duracionTotalString = "${(preferences.endMinutes - preferences.startMinutes) / 60} horas disponibles"

        return Itinerary(
            destino = preferences.destino.trim(),
            actividades = finalStops,
            duracionTotal = duracionTotalString,
            costoTotalEstimado = totalCostoText,
            puntoPartida = startPointName,
            rangoHorarioText = rangoHorarioText,
            incluyeComida = preferences.includeFoodStops,
            cantidadPersonas = preferences.cantidadPersonas
        )
    }

    private fun mapEstiloToVisualType(estilo: TourismInterest): ActivityVisualType {
        return when (estilo) {
            TourismInterest.CLASICO -> ActivityVisualType.DEFAULT
            TourismInterest.ALTERNATIVO -> ActivityVisualType.DEFAULT
            TourismInterest.MAINSTREAM -> ActivityVisualType.MAINSTREAM
            TourismInterest.CULTURAL -> ActivityVisualType.CULTURE
            TourismInterest.GASTRONOMICO -> ActivityVisualType.FOOD
            TourismInterest.NATURALEZA -> ActivityVisualType.NATURE
            TourismInterest.FAMILIAR -> ActivityVisualType.FAMILY
            TourismInterest.HISTORIA -> ActivityVisualType.HISTORY
            TourismInterest.COMPRAS -> ActivityVisualType.SHOPPING
            TourismInterest.FOTOGRAFIA -> ActivityVisualType.PHOTO
            TourismInterest.EVENTOS -> ActivityVisualType.EVENT
            TourismInterest.AVENTURA -> ActivityVisualType.ADVENTURE
        }
    }
}
