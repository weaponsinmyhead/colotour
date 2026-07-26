package com.colotour.app.domain.engine

import com.colotour.app.data.model.BudgetLevel
import com.colotour.app.data.model.MobilityType

class CostEstimator {
    fun estimateCosts(
        activities: List<PlannedActivity>,
        cantidadPersonas: Int,
        movilidadSeleccionada: Set<MobilityType>,
        presupuesto: BudgetLevel,
        comidasAgregadasCount: Int
    ): CostResult {
        if (presupuesto == BudgetLevel.GRATUITO) {
            return CostResult(
                totalCost = 0.0,
                activityCosts = activities.associate { it.place.id to 0.0 },
                transportCost = 0.0,
                foodCost = 0.0
            )
        }

        var totalActivitiesCost = 0.0
        val activityCosts = mutableMapOf<String, Double>()

        for (act in activities) {
            val cost = act.place.costoBasePorPersona * cantidadPersonas
            totalActivitiesCost += cost
            activityCosts[act.place.id] = cost
        }

        // Costo de transporte estimado promedio
        val numTransitos = if (activities.size > 1) activities.size - 1 else 0
        val averageTransportCost = if (movilidadSeleccionada.isEmpty()) {
            2.0
        } else {
            movilidadSeleccionada.map { mobility ->
                when (mobility) {
                    MobilityType.CAMINANDO -> 0.0
                    MobilityType.BICICLETA -> 1.5
                    MobilityType.TRANSPORTE_PUBLICO -> 2.0
                    MobilityType.AUTO -> 6.0
                    MobilityType.TAXI_APP -> 10.0
                    MobilityType.MIXTO -> 3.5
                }
            }.average()
        }
        val totalTransportCost = averageTransportCost * numTransitos * cantidadPersonas

        // Costo estimado de comida
        val costPerMeal = when (presupuesto) {
            BudgetLevel.GRATUITO -> 0.0
            BudgetLevel.BAJO -> 8.0
            BudgetLevel.MEDIO -> 20.0
            BudgetLevel.ALTO -> 45.0
        }
        val totalFoodCost = costPerMeal * comidasAgregadasCount * cantidadPersonas
        val totalCost = totalActivitiesCost + totalTransportCost + totalFoodCost

        return CostResult(
            totalCost = totalCost,
            activityCosts = activityCosts,
            transportCost = totalTransportCost,
            foodCost = totalFoodCost
        )
    }
}

data class CostResult(
    val totalCost: Double,
    val activityCosts: Map<String, Double>,
    val transportCost: Double,
    val foodCost: Double
)
