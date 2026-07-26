package com.colotour.app.domain.engine

import com.colotour.app.data.model.BudgetLevel
import com.colotour.app.data.model.TravelPreferences
import kotlin.math.abs

class PlaceScorer {
    fun scorePlace(place: CandidatePlace, preferences: TravelPreferences): Double {
        var score = 0.0

        // 1. Intereses Múltiples (Hasta 60 puntos)
        if (preferences.intereses.contains(place.estilo)) {
            score += 60.0
        } else {
            score += 10.0
        }

        // 2. Presupuesto (Hasta 30 puntos)
        if (preferences.presupuesto == BudgetLevel.GRATUITO) {
            if (place.presupuesto == BudgetLevel.GRATUITO) {
                score += 30.0
            } else {
                // Penalización por costos cuando se prefiere gratuito
                score -= 40.0
            }
        } else {
            if (place.presupuesto == preferences.presupuesto) {
                score += 30.0
            } else {
                val diff = abs(place.presupuesto.ordinal - preferences.presupuesto.ordinal)
                if (diff == 1) {
                    score += 15.0
                }
            }
        }

        // 3. Popularidad (Hasta 10 puntos)
        score += place.popularidad * 10.0

        return score
    }
}
