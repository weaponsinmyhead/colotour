package com.wayfii.app.data.model

import com.wayfii.app.R
import com.wayfii.app.domain.engine.discovery.AdventureDiscoveryEngine
import kotlinx.serialization.Serializable

@Serializable
data class SideQuestItem(
    val id: String,
    val title: String,
    val category: String, // e.g. "☕ Café de Especialidad", "🎨 Mural Oculto", "📷 Photo Spot"
    val description: String,
    val distanceDetourText: String, // e.g. "A 3 min de tu ruta"
    val iconEmoji: String,
    val latitud: Double?,
    val longitud: Double?,
    val isDiscovered: Boolean = false,
    val imageUrl: String? = null,
    val walkingTimeText: String = "3 min"
)

@Serializable
data class AdventureChapter(
    val chapterNumber: Int,
    val chapterTitle: String,
    val narrative: String,
    val discoveryText: String,
    val stopItem: ItineraryStop,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false
)

@Serializable
data class TravelPostcard(
    val id: String,
    val chapterTitle: String,
    val locationName: String,
    val heroImageUrl: String,
    val shortStory: String,
    val funFact: String,
    val unlockedDateText: String = "Buenos Aires 2026"
)

data class AdventureProposal(
    val id: String,
    val title: String,
    val emoji: String,
    val tagline: String,
    val durationText: String,
    val distanceText: String,
    val difficulty: String, // e.g. "Fácil & Relajada", "Moderada", "Exploratoria"
    val atmosphere: String, // e.g. "Relajante & Verde", "Urbana & Dinámica", "Acogedora & Cultural"
    val highlights: List<String>,
    val mainQuestStops: List<ItineraryStop>,
    val sideQuests: List<SideQuestItem>,
    val baseItinerary: Itinerary,
    val imageResId: Int = R.drawable.park_placeholder,
    val category: String = "Main Quest",
    val heroImageUrl: String? = null,
    val introNarrative: String = "",
    val isFavorite: Boolean = false,
    val adventureDna: AdventureDNA? = null,
    val contextEnvironment: ContextEnvironment? = null,
    val contextualReason: String = "",
    val adventureScore: AdventureScore? = null,
    val adventureTheme: AdventureTheme? = null,
    val activeCityMoment: CityMoment? = null
) {
    val chapters: List<AdventureChapter>
        get() = mainQuestStops.mapIndexed { index, stop ->
            AdventureChapter(
                chapterNumber = stop.order,
                chapterTitle = "Capítulo ${stop.order}: ${stop.titulo.replace("Inicio: ", "")}",
                narrative = stop.descripcion,
                discoveryText = stop.funFact.ifBlank { stop.historicalInfo.ifBlank { "Un rincón único de la ciudad." } },
                stopItem = stop,
                isUnlocked = index == 0,
                isCompleted = false
            )
        }
}

object AdventureProposalGenerator {

    private val discoveryEngine = AdventureDiscoveryEngine()

    fun generateProposals(baseItinerary: Itinerary, preferences: TravelPreferences): List<AdventureProposal> {
        return discoveryEngine.discoverAdventures(baseItinerary, preferences)
    }
}
