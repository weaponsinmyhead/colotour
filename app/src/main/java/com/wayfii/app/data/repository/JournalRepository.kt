package com.wayfii.app.data.repository

import com.wayfii.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JournalRepository {

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries.asStateFlow()

    private val _collections = MutableStateFlow<List<ThematicCollection>>(emptyList())
    val collections: StateFlow<List<ThematicCollection>> = _collections.asStateFlow()

    private val _memories = MutableStateFlow<List<JournalMemory>>(emptyList())
    val memories: StateFlow<List<JournalMemory>> = _memories.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val sampleEntries = listOf(
            JournalEntry(
                id = "j1",
                adventureTitle = "🌸 Buenos Aires en Flor",
                cityName = "Buenos Aires",
                completionDateFormatted = "2 de Agosto, 2026",
                heroImageUrl = "https://images.unsplash.com/photo-1522383225653-ed111181a951?q=80&w=1400&auto=format&fit=crop",
                narrative = "El perfume de los jazmines y la floración de los jacarandás transformaron la caminata por Recoleta y Palermo.\nUn recorrido sereno entre el Rosedal, pasajes con historia y un café de especialidad al aire libre.",
                distanceWalkedText = "3.2 km",
                durationText = "3.5 h",
                estimatedStepsText = "4,620 pasos",
                weatherBadge = "🌸 Primavera · ☀️ Soleado (22°C)",
                season = Season.SPRING,
                moodTag = "Romántica · Floral",
                passportStamp = PassportStamp("stamp_1", "Floración Jacarandá", "🌸", "ba_in_bloom", "02/08/2026", "#8E24AA"),
                mainQuestStops = listOf(
                    ItineraryStop(1, StopType.START, ActivityVisualType.NATURE, "10:00 AM", "Jardín Botánico Carlos Thays", "Invernaderos de hierro del siglo XIX y esculturas.", "1h 15m", "Gratis", funFact = "El invernadero principal fue traído desde París en 1898."),
                    ItineraryStop(2, StopType.PLACE, ActivityVisualType.CULTURE, "11:30 AM", "Rosedal de Palermo", "Más de 18.000 rosales en su máxima floración.", "45m", "Gratis", funFact = "El puente blanco de madera sobre el lago fue diseñado en 1914.")
                ),
                sideQuests = listOf(
                    SideQuestItem("sq1", "Hidden Courtyard", "☕ Patio Oculto", "Pequeño patio interno rodeado de parras.", "A 3 min", "🌿", -34.582, -58.419, true, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?q=80&w=1000&auto=format&fit=crop")
                ),
                personalNotes = "Día perfecto con luz cálida de tarde. El café de patio en el pasaje Russell fue un hallazgo memorable."
            ),
            JournalEntry(
                id = "j2",
                adventureTitle = "🍂 Caminata entre Hojas Doradas",
                cityName = "Buenos Aires",
                completionDateFormatted = "28 de Mayo, 2026",
                heroImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop",
                narrative = "Las copas de los plátanos tiñeron de oro las veredas empedradas de San Telmo y Montserrat.\nCaminata pausada con parada en librerías de antiguo y aroma a café tostado artesanal.",
                distanceWalkedText = "2.8 km",
                durationText = "2.5 h",
                estimatedStepsText = "4,100 pasos",
                weatherBadge = "🍂 Otoño · ☁️ Nublado (16°C)",
                season = Season.AUTUMN,
                moodTag = "Nostálgica · Cálida",
                passportStamp = PassportStamp("stamp_2", "Hojas Doradas", "🍂", "autumn_walk", "28/05/2026", "#D97706"),
                mainQuestStops = listOf(
                    ItineraryStop(1, StopType.START, ActivityVisualType.HISTORY, "03:00 PM", "Plaza Dorrego & Anticuarios", "Epicentro histórico de San Telmo.", "1h", "Gratis"),
                    ItineraryStop(2, StopType.FOOD, ActivityVisualType.FOOD, "04:15 PM", "Café de Casona Reciclada", "Café de especialidad en patio de 1920.", "45m", "$$")
                ),
                sideQuests = emptyList(),
                personalNotes = "Compré una primera edición antigua en el pasaje de los anticuarios."
            ),
            JournalEntry(
                id = "j3",
                adventureTitle = "☕ Refugios de Invierno",
                cityName = "Buenos Aires",
                completionDateFormatted = "15 de Julio, 2026",
                heroImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=1400&auto=format&fit=crop",
                narrative = "Travesía entre cafés notables centenarios, galerías de arte techadas y chocolate caliente a resguardo del aire frío.",
                distanceWalkedText = "2.1 km",
                durationText = "2.0 h",
                estimatedStepsText = "3,250 pasos",
                weatherBadge = "❄️ Invierno · 🧊 Frío (11°C)",
                season = Season.WINTER,
                moodTag = "Acogedora · Intima",
                passportStamp = PassportStamp("stamp_3", "Refugio de Invierno", "☕", "coffee_corners", "15/07/2026", "#78350F"),
                mainQuestStops = listOf(
                    ItineraryStop(1, StopType.START, ActivityVisualType.CULTURE, "04:00 PM", "Café Tortoni", "Café notable emblemático de 1858.", "1h", "$$")
                ),
                sideQuests = emptyList(),
                personalNotes = "El chocolate con churros en la sala de los espejos estuvo excelente."
            )
        )

        val sampleCollections = listOf(
            ThematicCollection("col_1", "Cafés Notables & Históricos", "☕", 5, 20, "Casonas y salones de café conservados desde 1900."),
            ThematicCollection("col_2", "Jardines & Rosadales", "🌿", 8, 15, "Parques botánicos, rosadales y patios ajardinados."),
            ThematicCollection("col_3", "Museos & Galerías", "🏛️", 4, 12, "Palacios de arte e historia viva de la ciudad."),
            ThematicCollection("col_4", "Murales & Arte Urbano", "🎨", 6, 15, "Pasajes coloridos e intervenciones urbanas internacionales."),
            ThematicCollection("col_5", "Librerías de Autor", "📚", 3, 10, "Ediciones raras, cúpulas de lectura y aroma a papel antiguo.")
        )

        val sampleMemories = listOf(
            JournalMemory("mem_1", "Hace un año hoy...", "Tu primera caminata primaveral por el Rosedal de Palermo.", "2 de Agosto, 2025", "🌸"),
            JournalMemory("mem_2", "Descubridor de Cafés", "Completaste 12 paradas en cafés históricos este año.", "Julio 2026", "☕")
        )

        _entries.value = sampleEntries
        _collections.value = sampleCollections
        _memories.value = sampleMemories
    }

    fun addCompletedAdventure(proposal: AdventureProposal) {
        val entry = JournalEntry(
            id = "journal_${System.currentTimeMillis()}",
            adventureTitle = proposal.title,
            cityName = if (proposal.baseItinerary.destino.isNotBlank()) proposal.baseItinerary.destino else "Buenos Aires",
            completionDateFormatted = "Hoy, 2026",
            heroImageUrl = proposal.heroImageUrl ?: "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1400&auto=format&fit=crop",
            narrative = proposal.introNarrative.ifBlank { proposal.tagline },
            distanceWalkedText = proposal.distanceText,
            durationText = proposal.durationText,
            estimatedStepsText = "4,850 pasos",
            weatherBadge = proposal.contextEnvironment?.descriptionBadge ?: "🌸 Primavera · ☀️ Soleado",
            season = proposal.contextEnvironment?.season ?: Season.SPRING,
            moodTag = proposal.atmosphere,
            passportStamp = PassportStamp(
                id = "stamp_${System.currentTimeMillis()}",
                title = proposal.adventureTheme?.completionBadgeTitle ?: proposal.title,
                emoji = proposal.emoji,
                themeId = proposal.adventureTheme?.id ?: "custom",
                unlockedDateText = "Hoy, 2026",
                badgeColorHex = proposal.adventureDna?.colorPalette?.primaryHex ?: "#00897B"
            ),
            mainQuestStops = proposal.mainQuestStops,
            sideQuests = proposal.sideQuests,
            personalNotes = "Una experiencia inolvidable guardada en mi diario de viaje."
        )

        _entries.value = listOf(entry) + _entries.value
    }
}
