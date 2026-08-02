package com.wayfii.app.domain.engine.director

import com.wayfii.app.data.model.*

class AdventureDirector {

    fun evaluateLiveAdaptation(
        proposal: AdventureProposal,
        activeChapterOrder: Int,
        elapsedMinutes: Int,
        simulatedRain: Boolean = false,
        simulatedSunset: Boolean = false,
        simulatedSurprise: Boolean = false
    ): AdventureAdaptation {
        val currentStop = proposal.mainQuestStops.find { it.order == activeChapterOrder }
            ?: proposal.mainQuestStops.firstOrNull()

        var liveNarrative = currentStop?.descripcion ?: proposal.introNarrative
        var palette = proposal.adventureDna?.colorPalette ?: AdventureColorPalette("#00897B", "#00BFA5", "#E0F2F1", "#F8FAFC")
        var surpriseMoment: DirectorSurpriseMoment? = null
        var isAdapted = false
        var reason = ""

        // 1. SUDDEN RAIN ADAPTATION
        if (simulatedRain) {
            isAdapted = true
            reason = "☔ Adapta la ruta a galerías y salones resguardados por lluvia"
            liveNarrative = "Comienza una garúa suave sobre los empedrados de la ciudad.\nEl recorrido se resguarda ahora bajo los pasajes techados y galerías históricas a paso sereno."
            palette = AdventureColorPalette("#2563EB", "#1E40AF", "#DBEAFE", "#F8FAFC")
            surpriseMoment = DirectorSurpriseMoment(
                id = "surp_rain",
                title = "☕ Salón de Té Resguardado",
                narrative = "Galería techada de 1900 a solo 50 metros para resguardarte de la lluvia con aroma a café.",
                type = SurpriseType.REST_SPOT,
                iconEmoji = "☔",
                actionText = "Ver Refugio"
            )
        }
        // 2. GOLDEN HOUR / SUNSET PROXIMITY ADAPTATION
        else if (simulatedSunset) {
            isAdapted = true
            reason = "🌅 La luz dorada del atardecer ilumina la ruta"
            liveNarrative = "La luz dorada de la tarde empieza a encender las cúpulas señoriales y copas de los árboles.\nEs el momento perfecto para ascender al mirador y contemplar el horizonte."
            palette = AdventureColorPalette("#EA580C", "#C2410C", "#FFEDD5", "#FFF7ED")
            surpriseMoment = DirectorSurpriseMoment(
                id = "surp_sunset",
                title = "📷 Mirador Secreto de Cúpulas",
                narrative = "La luz poniente pega directo sobre los vitrales de la cúpula señorial frente a vos.",
                type = SurpriseType.PHOTO_SPOT,
                iconEmoji = "🌅",
                actionText = "Tomar Foto"
            )
        }
        // 3. EXPLICIT SURPRISE MOMENT TRIGGER
        else if (simulatedSurprise) {
            isAdapted = true
            reason = "✨ Momento sorpresa contextual descubierto"
            surpriseMoment = DirectorSurpriseMoment(
                id = "surp_curiosity",
                title = "📜 Curiosidad de Época",
                narrative = "¿Sabías que este farol victoriano fue traído en barco desde Gran Bretaña en 1895?",
                type = SurpriseType.CURIOSITY,
                iconEmoji = "📜",
                actionText = "Leer Historia"
            )
        }
        // 4. PACE / UNHURRIED ADAPTATION (If elapsed time is long)
        else if (elapsedMinutes > 45) {
            isAdapted = true
            reason = "🌿 Ritmo pausado y contemplativo"
            liveNarrative = "El ritmo sereno de la tarde te permite disfrutar la caminata sin prisa.\nContinuemos al siguiente capítulo a tu propio paso."
        }

        // Adapt remaining stops if raining (Make them indoor)
        val adaptedStops = if (simulatedRain) {
            proposal.mainQuestStops.map { stop ->
                stop.copy(
                    descripcion = if (stop.descripcion.contains("resguard", ignoreCase = true)) stop.descripcion
                    else "Pasaje techado resguardado de la lluvia: ${stop.descripcion}"
                )
            }
        } else {
            proposal.mainQuestStops
        }

        return AdventureAdaptation(
            isAdapted = isAdapted,
            adaptationReason = reason,
            liveNarrative = liveNarrative,
            activeSurpriseMoment = surpriseMoment,
            colorPalette = palette,
            adaptedStops = adaptedStops
        )
    }
}
