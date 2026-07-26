package com.wayfii.app.domain.engine

import com.wayfii.app.data.model.BudgetLevel
import com.wayfii.app.data.model.MobilityType
import com.wayfii.app.data.model.TourismInterest
import com.wayfii.app.data.model.TravelPreferences
import kotlin.math.abs

class PlaceScorer {
    fun scorePlace(
        place: CandidatePlace,
        preferences: TravelPreferences,
        seenInterests: Set<TourismInterest> = emptySet()
    ): Double {
        var score = 0.0

        // 1. Intereses Múltiples (Hasta 60 puntos)
        val matchesInterest = preferences.intereses.contains(place.estilo)
        
        // Sinergia Aventura <-> Naturaleza
        val isAventuraNaturalezaSinergia = (preferences.intereses.contains(TourismInterest.NATURALEZA) && place.estilo == TourismInterest.AVENTURA) ||
                (preferences.intereses.contains(TourismInterest.AVENTURA) && place.estilo == TourismInterest.NATURALEZA)

        if (matchesInterest) {
            score += 60.0
            if (seenInterests.contains(place.estilo)) {
                score -= 25.0
            }
        } else if (isAventuraNaturalezaSinergia) {
            score += 40.0 // Fuerte afinidad por sinergia outdoor
            if (seenInterests.contains(place.estilo)) {
                score -= 15.0
            }
        } else {
            score += 10.0
        }

        // 2. Presupuesto (Hasta 30 puntos)
        if (preferences.presupuesto == BudgetLevel.GRATUITO) {
            if (place.presupuesto == BudgetLevel.GRATUITO) {
                score += 30.0
                // Priorizar parques, playas, miradores, senderos y circuitos
                val nameLower = place.nombre.lowercase()
                val descLower = place.descripcion.lowercase()
                if (nameLower.contains("parque") || nameLower.contains("playa") || nameLower.contains("mirador") ||
                    nameLower.contains("sendero") || nameLower.contains("circuito") ||
                    descLower.contains("parque") || descLower.contains("playa") || descLower.contains("mirador") ||
                    descLower.contains("sendero") || descLower.contains("circuito")
                ) {
                    score += 15.0
                }
            } else {
                score -= 40.0
            }
        } else {
            // Un lugar gratuito es perfectamente compatible con presupuestos BAJO, MEDIO y ALTO
            if (place.presupuesto == BudgetLevel.GRATUITO || place.presupuesto == preferences.presupuesto) {
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

        // 4. Boost por movilidad activa
        val nameLower = place.nombre.lowercase()
        val descLower = place.descripcion.lowercase()

        if (preferences.movilidad.contains(MobilityType.CAMINANDO)) {
            val isTrekkingOrWalking = place.estilo == TourismInterest.AVENTURA ||
                    place.estilo == TourismInterest.NATURALEZA ||
                    nameLower.contains("trekking") || nameLower.contains("caminata") || nameLower.contains("mirador") ||
                    descLower.contains("trekking") || descLower.contains("caminata") || descLower.contains("mirador")
            if (isTrekkingOrWalking) {
                score += 15.0
            }
        }

        if (preferences.movilidad.contains(MobilityType.BICICLETA)) {
            val isBikeFriendly = nameLower.contains("bicicleta") || nameLower.contains("ciclo") ||
                    descLower.contains("bicicleta") || descLower.contains("ciclo")
            if (isBikeFriendly) {
                score += 15.0
            }
        }

        return score
    }
}
