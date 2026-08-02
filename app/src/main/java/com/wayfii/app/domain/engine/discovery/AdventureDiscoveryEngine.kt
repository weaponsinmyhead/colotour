package com.wayfii.app.domain.engine.discovery

import com.wayfii.app.R
import com.wayfii.app.data.model.*
import com.wayfii.app.domain.engine.context.ContextEngine

class AdventureDiscoveryEngine {

    private val contextEngine = ContextEngine()
    private val momentDetector = CityMomentDetector()
    private val scoreEvaluator = AdventureScoreEvaluator()

    fun discoverAdventures(
        baseItinerary: Itinerary,
        preferences: TravelPreferences
    ): List<AdventureProposal> {
        val destination = if (baseItinerary.destino.isNotBlank()) baseItinerary.destino else "Buenos Aires"

        // ────── STEP 1: READ CONTEXT ──────
        val contextEnv = contextEngine.resolveContext(preferences)

        // ────── STEP 2: DETECT ACTIVE CITY MOMENTS ──────
        val activeMoments = momentDetector.detectActiveMoments(contextEnv)
        val primaryMoment = activeMoments.firstOrNull()

        // ────── STEP 3, 4, 5, 6, 7 & 8: PIPELINE PER ADVENTURE ──────
        val proposals = mutableListOf<AdventureProposal>()

        // 1. PRIMARY MOMENT / SEASONAL ADVENTURE
        val theme1 = when (contextEnv.season) {
            Season.SPRING -> AdventureTheme.BUENOS_AIRES_BLOOM
            Season.SUMMER -> AdventureTheme.HIDDEN_GARDENS
            Season.AUTUMN -> AdventureTheme.AUTUMN_WALK
            Season.WINTER -> AdventureTheme.COFFEE_CORNERS
        }

        val reason1 = primaryMoment?.contextualReasonLabel
            ?: when (contextEnv.season) {
                Season.SPRING -> "🌸 Solo durante la floración de Jacarandás"
                Season.SUMMER -> "☀️ Sombra y brisa veraniega hoy"
                Season.AUTUMN -> "🍂 Exclusivo de la temporada de hojas doradas"
                Season.WINTER -> "☕ Refugio de invierno con historia"
            }

        val proposal1 = createProposalFromTheme(
            id = "discovery_1",
            theme = theme1,
            contextEnv = contextEnv,
            destination = destination,
            contextualReason = reason1,
            activeMoment = primaryMoment,
            baseItinerary = baseItinerary
        )
        proposals.add(proposal1)

        // 2. TIME OF DAY / WEATHER SPECIAL ADVENTURE
        val (theme2, reason2) = when {
            contextEnv.weather == WeatherCondition.RAIN -> Pair(
                AdventureTheme.HISTORIC_JOURNEY,
                "☔ 100% resguardado bajo techo ideal para lluvia"
            )
            contextEnv.timeOfDay == TimeOfDay.GOLDEN_HOUR -> Pair(
                AdventureTheme.GOLDEN_HOUR_ESCAPE,
                "🌅 Recomendado al atardecer para fotos perfectas"
            )
            contextEnv.timeOfDay == TimeOfDay.NIGHT -> Pair(
                AdventureTheme.BUENOS_AIRES_AFTER_DARK,
                "🌙 Luces nocturnas y arquitectura iluminada"
            )
            else -> Pair(
                AdventureTheme.URBAN_EXPLORER,
                "🎨 Arte urbano y pasajes creativos"
            )
        }

        val proposal2 = createProposalFromTheme(
            id = "discovery_2",
            theme = theme2,
            contextEnv = contextEnv,
            destination = destination,
            contextualReason = reason2,
            activeMoment = null,
            baseItinerary = baseItinerary
        )
        proposals.add(proposal2)

        // 3. GOURMET / COFFEE & CORNERS CLASSIC
        val proposal3 = createProposalFromTheme(
            id = "discovery_3",
            theme = AdventureTheme.COFFEE_CORNERS,
            contextEnv = contextEnv,
            destination = destination,
            contextualReason = "☕ Perfecto para disfrutar sin prisa hoy",
            activeMoment = null,
            baseItinerary = baseItinerary
        )
        proposals.add(proposal3)

        // 4. HISTORIC JOURNEY
        val proposal4 = createProposalFromTheme(
            id = "discovery_4",
            theme = AdventureTheme.HISTORIC_JOURNEY,
            contextEnv = contextEnv,
            destination = destination,
            contextualReason = "🏛️ Los íconos y casonas históricas de $destination",
            activeMoment = null,
            baseItinerary = baseItinerary
        )
        proposals.add(proposal4)

        // Sort all proposals by dynamic AdventureScore descending
        return proposals.sortedByDescending { it.adventureScore?.totalScore ?: 0.0 }
    }

    private fun createProposalFromTheme(
        id: String,
        theme: AdventureTheme,
        contextEnv: ContextEnvironment,
        destination: String,
        contextualReason: String,
        activeMoment: CityMoment?,
        baseItinerary: Itinerary
    ): AdventureProposal {
        // STEP 4: Write Narrative (STORY FIRST)
        val narrative = when (theme.id) {
            "ba_in_bloom" -> "El perfume de los jazmines y la floración púrpura de los jacarandás tiñen las veredas de $destination.\nUn recorrido mágico pensado para disfrutarse a pleno sol entre parques y salones de té."
            "autumn_walk" -> "Las copas doradas de los fresnos y plátanos alfombran las veredas empedradas de $destination.\nCaminá despacio entre librerías con aroma a café y pasajes iluminados por la luz cálida del atardecer."
            "golden_hour" -> "Experimentá los minutos mágicos donde el sol poniente enciende las cúpulas históricas de $destination.\nUna travesía que culmina en terrazas mirador antes de que caiga la noche."
            "ba_after_dark" -> "Cuando cae el sol, la arquitectura señorial y la vida nocturna despiertan en $destination.\nSumergite en bares ocultos speakeasy, clubes de jazz tenue y avenidas iluminadas."
            "hidden_gardens" -> "Descubrí el oasis verde escondido de $destination.\nPasajes botánicos, patios ajardinados de casonas señoriales y pequeños cafés donde el tiempo se detiene."
            "coffee_corners" -> "Sabor, literatura y pausa urbana en $destination.\nUn recorrido pensado para disfrutarse sorbo a sorbo entre casonas históricas y aromas de tostado artesanal."
            "urban_explorer" -> "Senti la energía libre y creativa de $destination.\nDesde intervenciones de arte urbano internacional hasta callejones de ladrillo y galerías independientes."
            else -> "Los íconos imprescindibles y la esencia viva de $destination.\nMonumentos señoriales, pasajes de época y plazas llenas de historia viva."
        }

        val heroImage = when (theme.id) {
            "ba_in_bloom" -> "https://images.unsplash.com/photo-1522383225653-ed111181a951?q=80&w=1400&auto=format&fit=crop"
            "autumn_walk" -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop"
            "golden_hour" -> "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop"
            "ba_after_dark" -> "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=1400&auto=format&fit=crop"
            "hidden_gardens" -> "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1400&auto=format&fit=crop"
            "coffee_corners" -> "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=1400&auto=format&fit=crop"
            "urban_explorer" -> "https://images.unsplash.com/photo-1561055657-b9e0bf0fa360?q=80&w=1400&auto=format&fit=crop"
            else -> "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1400&auto=format&fit=crop"
        }

        val highlights = when (theme.id) {
            "ba_in_bloom" -> listOf("Jacarandás en Flor", "Rosedal de Palermo", "Café de Patio al Aire Libre")
            "autumn_walk" -> listOf("Senderos de Hojas Doradas", "Librería de Anticuario", "Café de Especialidad")
            "golden_hour" -> listOf("Mirador Panorámico", "Paseo del Río", "Brindis al Atardecer")
            "ba_after_dark" -> listOf("Bar Speakeasy", "Club de Jazz", "Cúpulas Iluminadas")
            "hidden_gardens" -> listOf("Jardín Botánico", "Pasaje Russell", "Patio Verde")
            "coffee_corners" -> listOf("Café Notable", "Librería Clásica", "Pasaje Histórico")
            "urban_explorer" -> listOf("Murales de Autor", "Galería de Arte", "Feria de Diseño")
            else -> listOf("Plaza Principal", "Cabildo Histórico", "Palacio de Época")
        }

        // STEP 6: Generate Main Quest
        val mainStops = if (baseItinerary.actividades.isNotEmpty()) {
            baseItinerary.actividades
        } else {
            listOf(
                ItineraryStop(
                    order = 1,
                    type = StopType.START,
                    visualType = ActivityVisualType.NATURE,
                    horaInicio = "10:00 AM",
                    titulo = theme.title,
                    descripcion = narrative,
                    duracionEstimada = "1h 15m",
                    costoEstimado = "Gratis"
                )
            )
        }

        // STEP 7: Generate Side Quests
        val sideQuests = listOf(
            SideQuestItem(
                id = "sq_${id}_1",
                title = "${theme.sideQuestStyle} Oculto",
                category = "✨ Side Quest",
                description = "Desvío secreto a solo 3 minutos para tomar fotos únicas.",
                distanceDetourText = "A 3 min",
                iconEmoji = theme.iconEmoji,
                latitud = -34.582,
                longitud = -58.419,
                walkingTimeText = "3 min",
                imageUrl = heroImage
            )
        )

        // STEP 8: Generate UI Metadata & AdventureScore
        val dna = AdventureDNA(
            title = theme.title,
            mood = theme.mood,
            narrative = narrative,
            categoryTag = theme.timelineStyle,
            heroImageUrl = heroImage,
            colorPalette = theme.colorPalette,
            photographyStyle = theme.heroPhotographyStyle,
            highlights = highlights,
            sideQuestTheme = theme.sideQuestStyle,
            badgeText = contextualReason,
            iconEmoji = theme.iconEmoji,
            microcopy = theme.narrativeTone
        )

        val proposalDraft = AdventureProposal(
            id = id,
            title = theme.title,
            emoji = theme.iconEmoji,
            tagline = contextualReason,
            durationText = baseItinerary.duracionTotal.ifBlank { "3.5 h" },
            distanceText = "3.2 km",
            difficulty = "Ideal hoy",
            atmosphere = theme.mood,
            highlights = highlights,
            mainQuestStops = mainStops,
            sideQuests = sideQuests,
            baseItinerary = baseItinerary,
            imageResId = R.drawable.park_placeholder,
            category = theme.timelineStyle,
            heroImageUrl = heroImage,
            introNarrative = narrative,
            adventureDna = dna,
            contextEnvironment = contextEnv,
            contextualReason = contextualReason,
            adventureTheme = theme,
            activeCityMoment = activeMoment
        )

        val score = scoreEvaluator.evaluateScore(proposalDraft, contextEnv, theme, activeMoment)
        return proposalDraft.copy(adventureScore = score)
    }
}
