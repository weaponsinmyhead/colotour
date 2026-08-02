package com.wayfii.app.domain.engine

import com.wayfii.app.data.model.*
import kotlin.math.abs

class PlaceScorer {
    fun scorePlace(
        place: CandidatePlace,
        preferences: TravelPreferences,
        seenInterests: Set<TourismInterest> = emptySet(),
        context: ContextEnvironment? = null
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

        // 5. CONTEXTUAL INTELLIGENCE BOOSTS & PENALTIES
        if (context != null) {
            // A. Weather Adaptations
            if (context.weather == WeatherCondition.RAIN) {
                val isIndoor = place.estilo == TourismInterest.HISTORIA ||
                        place.estilo == TourismInterest.CULTURAL ||
                        place.estilo == TourismInterest.GASTRONOMICO ||
                        nameLower.contains("museo") || nameLower.contains("café") || nameLower.contains("galería") ||
                        nameLower.contains("pasaje") || nameLower.contains("librería") || nameLower.contains("teatro") ||
                        descLower.contains("cubierto") || descLower.contains("techo") || descLower.contains("interior")
                if (isIndoor) {
                    score += 35.0
                } else if (place.estilo == TourismInterest.NATURALEZA || nameLower.contains("parque") || nameLower.contains("jardín")) {
                    score -= 30.0
                }
            }

            if (context.weather == WeatherCondition.VERY_HOT || context.season == Season.SUMMER) {
                val isCoolOrShaded = nameLower.contains("helad") || nameLower.contains("río") || nameLower.contains("costa") ||
                        nameLower.contains("sombra") || descLower.contains("helado") || descLower.contains("agua")
                if (isCoolOrShaded) {
                    score += 25.0
                }
            }

            // B. Time of Day Adaptations
            if (context.timeOfDay == TimeOfDay.GOLDEN_HOUR) {
                val isSunsetSpot = nameLower.contains("mirador") || nameLower.contains("río") || nameLower.contains("terraza") ||
                        place.estilo == TourismInterest.FOTOGRAFIA || descLower.contains("puesta de sol") || descLower.contains("vista")
                if (isSunsetSpot) {
                    score += 30.0
                }
            }

            if (context.timeOfDay == TimeOfDay.NIGHT) {
                val isNightSpot = place.estilo == TourismInterest.GASTRONOMICO || place.estilo == TourismInterest.EVENTOS ||
                        nameLower.contains("bar") || nameLower.contains("jazz") || nameLower.contains("speakeasy") ||
                        descLower.contains("nocturn") || descLower.contains("música")
                if (isNightSpot) {
                    score += 35.0
                } else if (nameLower.contains("jardín") || nameLower.contains("museo")) {
                    score -= 20.0
                }
            }

            // C. Seasonal Adaptations
            if (context.season == Season.SPRING) {
                val isSpringSpot = place.estilo == TourismInterest.NATURALEZA || nameLower.contains("botánico") ||
                        nameLower.contains("jardín") || nameLower.contains("rosas") || nameLower.contains("parque")
                if (isSpringSpot) {
                    score += 30.0
                }
            }

            if (context.season == Season.AUTUMN) {
                val isAutumnSpot = nameLower.contains("hojas") || nameLower.contains("librería") || nameLower.contains("pasaje") ||
                        place.estilo == TourismInterest.HISTORIA || place.estilo == TourismInterest.FOTOGRAFIA
                if (isAutumnSpot) {
                    score += 25.0
                }
            }

            if (context.season == Season.WINTER) {
                val isCozyWinterSpot = nameLower.contains("café") || nameLower.contains("historico") || nameLower.contains("teatro") ||
                        nameLower.contains("biblioteca") || place.estilo == TourismInterest.CULTURAL
                if (isCozyWinterSpot) {
                    score += 30.0
                }
            }
        }

        return score
    }
}
