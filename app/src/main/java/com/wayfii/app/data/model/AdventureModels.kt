package com.wayfii.app.data.model

import com.wayfii.app.R
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
    val isDiscovered: Boolean = false
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
    val imageResId: Int = R.drawable.park_placeholder
)

object AdventureProposalGenerator {

    fun generateProposals(baseItinerary: Itinerary, preferences: TravelPreferences): List<AdventureProposal> {
        val stops = baseItinerary.actividades
        val destination = if (baseItinerary.destino.isNotBlank()) baseItinerary.destino else "la ciudad"

        val defaultMainStops = if (stops.isNotEmpty()) stops else listOf(
            ItineraryStop(1, StopType.START, ActivityVisualType.START, "09:00", "Punto de Inicio", "Comenzá tu aventura", "30 min", "Gratis"),
            ItineraryStop(2, StopType.PLACE, ActivityVisualType.CULTURE, "09:30", "Centro Histórico", "Paseo por calles emblemáticas", "1 hr", "Gratis"),
            ItineraryStop(3, StopType.FOOD, ActivityVisualType.FOOD, "11:00", "Café Local", "Pausa reconfortante", "45 min", "$"),
            ItineraryStop(4, StopType.PLACE, ActivityVisualType.NATURE, "12:00", "Mirador Urbano", "Vistas panorámicas increíbles", "1 hr", "Gratis")
        )

        val sampleSideQuests = listOf(
            SideQuestItem("sq1", "Mural Artístico Secreto", "🎨 Mural Oculto", "Arte urbano escondido en una callejuela", "A 2 min", "🎨", (defaultMainStops.firstOrNull()?.latitud ?: -34.6037) + 0.001, (defaultMainStops.firstOrNull()?.longitud ?: -58.3816) + 0.001),
            SideQuestItem("sq2", "Cafetería de Especialidad", "☕ Café Secreto", "Famosa por sus granos de origen y tostado artesanal", "A 4 min", "☕", (defaultMainStops.getOrNull(1)?.latitud ?: -34.6037) - 0.001, (defaultMainStops.getOrNull(1)?.longitud ?: -58.3816) + 0.002),
            SideQuestItem("sq3", "Rincón Fotográfico Top", "📷 Photo Spot", "Encuadre perfecto con luz natural increíble", "A 3 min", "📷", (defaultMainStops.getOrNull(2)?.latitud ?: -34.6037) + 0.002, (defaultMainStops.getOrNull(2)?.longitud ?: -58.3816) - 0.001),
            SideQuestItem("sq4", "Galería de Artesanos", "🛍 Comercio Local", "Pequeño patio lleno de diseño independiente", "A 5 min", "🛍", (defaultMainStops.lastOrNull()?.latitud ?: -34.6037) - 0.002, (defaultMainStops.lastOrNull()?.longitud ?: -58.3816) - 0.002)
        )

        // 1. Hidden Gardens -> park_placeholder
        val proposal1 = AdventureProposal(
            id = "hidden_gardens",
            title = "Hidden Gardens",
            emoji = "🌿",
            tagline = "Parques tranquilos, calles escondidas y cafés de patio verde en $destination.",
            durationText = baseItinerary.duracionTotal.ifBlank { "3.5 hrs" },
            distanceText = "2.8 km",
            difficulty = "Fácil & Relajada",
            atmosphere = "Serena, Verde, Aire Libre",
            highlights = listOf("Pasajes Botánicos", "Café de Patio", "Senderos Arbolados"),
            mainQuestStops = defaultMainStops.filter { it.visualType == ActivityVisualType.NATURE || it.visualType == ActivityVisualType.START || it.type == StopType.PLACE }.ifEmpty { defaultMainStops },
            sideQuests = listOf(sampleSideQuests[0], sampleSideQuests[1]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.park_placeholder
        )

        // 2. Urban Explorer -> streetart_placeholder
        val proposal2 = AdventureProposal(
            id = "urban_explorer",
            title = "Urban Explorer",
            emoji = "🔥",
            tagline = "Street art, pasajes alternativos y rincones vibrantes de la escena local.",
            durationText = "4 hrs",
            distanceText = "4.2 km",
            difficulty = "Exploratoria & Activa",
            atmosphere = "Vibrante, Moderna, Sorprendente",
            highlights = listOf("Graffitis Famosos", "Arquitectura Local", "Mercados Urbanos"),
            mainQuestStops = defaultMainStops,
            sideQuests = listOf(sampleSideQuests[0], sampleSideQuests[2], sampleSideQuests[3]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.streetart_placeholder
        )

        // 3. Coffee & Corners -> cafe_placeholder
        val proposal3 = AdventureProposal(
            id = "coffee_corners",
            title = "Coffee & Corners",
            emoji = "☕",
            tagline = "Librerías de autor, cafés de aroma intenso y calles empedradas para caminar sin prisa.",
            durationText = "2.5 hrs",
            distanceText = "1.9 km",
            difficulty = "Paseo Calmo",
            atmosphere = "Acogedora, Bohemia, Cálida",
            highlights = listOf("Cafetería de Especialidad", "Pasajes Históricos", "Librería Clásica"),
            mainQuestStops = defaultMainStops.filter { it.type == StopType.FOOD || it.visualType == ActivityVisualType.CULTURE || it.type == StopType.START }.ifEmpty { defaultMainStops },
            sideQuests = listOf(sampleSideQuests[1], sampleSideQuests[3]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.cafe_placeholder
        )

        // 4. First Time in the City -> landmarks_placeholder
        val proposal4 = AdventureProposal(
            id = "first_time",
            title = "First Time in $destination",
            emoji = "🏛",
            tagline = "Los íconos imprescindibles y la esencia de $destination en una primera mirada inolvidable.",
            durationText = "3 hrs",
            distanceText = "3.1 km",
            difficulty = "Equilibrada",
            atmosphere = "Monumental, Clásica, Emblemática",
            highlights = listOf("Monumentos Clásicos", "Plazas Principales", "Vistas Panorámicas"),
            mainQuestStops = defaultMainStops,
            sideQuests = listOf(sampleSideQuests[0], sampleSideQuests[1], sampleSideQuests[2]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.landmarks_placeholder
        )

        // 5. Golden Hour Escape -> sunset_placeholder
        val proposal5 = AdventureProposal(
            id = "golden_hour",
            title = "Golden Hour Escape",
            emoji = "🌅",
            tagline = "Ruta diseñada para culminar en un mirador ideal justo durante la puesta de sol.",
            durationText = "3 hrs",
            distanceText = "2.5 km",
            difficulty = "Romántica & Mágica",
            atmosphere = "Atardecer, Vista Abierta, Fotogénica",
            highlights = listOf("Ruta del Sol", "Mirador Elevado", "Brindis al Atardecer"),
            mainQuestStops = defaultMainStops,
            sideQuests = listOf(sampleSideQuests[2], sampleSideQuests[3]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.sunset_placeholder
        )

        return listOf(proposal1, proposal2, proposal3, proposal4, proposal5)
    }
}
