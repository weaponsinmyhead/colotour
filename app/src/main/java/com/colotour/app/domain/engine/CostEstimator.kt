package com.colotour.app.domain.engine

import com.colotour.app.data.model.BudgetLevel
import com.colotour.app.data.model.MobilityType

class CostEstimator {
    fun estimateCosts(
        activities: List<PlannedStop>,
        cantidadPersonas: Int,
        movilidadSeleccionada: Set<MobilityType>,
        presupuesto: BudgetLevel
    ): CostResult {
        var totalActivitiesCost = 0.0
        val activityCosts = mutableMapOf<String, Double>()
        var foodCost = 0.0

        val foodStops = activities.filterIsInstance<PlannedStop.FoodStop>()
        val placeStops = activities.filterIsInstance<PlannedStop.PlaceStop>()

        // 1. Costo estimado de comida
        val costPerMeal = when (presupuesto) {
            BudgetLevel.GRATUITO -> 0.0
            BudgetLevel.BAJO -> 8.0
            BudgetLevel.MEDIO -> 20.0
            BudgetLevel.ALTO -> 45.0
        }
        foodCost = costPerMeal * foodStops.size * cantidadPersonas

        // 2. Costo de atracciones
        if (presupuesto != BudgetLevel.GRATUITO) {
            for (act in placeStops) {
                val cost = act.place.costoBasePorPersona * cantidadPersonas
                totalActivitiesCost += cost
                activityCosts[act.place.id] = cost
            }
        } else {
            // Si es gratuito, todas las atracciones son costo 0
            for (act in placeStops) {
                activityCosts[act.place.id] = 0.0
            }
        }

        // 3. Costo de transporte
        val numTransitos = if (activities.size > 1) activities.size - 1 else 0
        val averageTransportCost = if (movilidadSeleccionada.isEmpty()) {
            2.0
        } else {
            movilidadSeleccionada.map { mobility ->
                when (mobility) {
                    MobilityType.CAMINANDO -> 0.0
                    MobilityType.BICICLETA -> 0.0 // Caminando y Bici mantienen costo 0
                    MobilityType.TRANSPORTE_PUBLICO -> 2.0
                    MobilityType.AUTO -> 6.0
                    MobilityType.TAXI_APP -> 10.0
                    MobilityType.MIXTO -> 3.5
                }
            }.average()
        }
        val totalTransportCost = averageTransportCost * numTransitos * cantidadPersonas

        val totalCost = totalActivitiesCost + totalTransportCost + foodCost

        return CostResult(
            totalCost = totalCost,
            activityCosts = activityCosts,
            transportCost = totalTransportCost,
            foodCost = foodCost
        )
    }
}

data class CostResult(
    val totalCost: Double,
    val activityCosts: Map<String, Double>,
    val transportCost: Double,
    val foodCost: Double
)

