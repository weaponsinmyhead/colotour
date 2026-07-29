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
    val isDiscovered: Boolean = false,
    val imageUrl: String? = null,
    val walkingTimeText: String = "3 min"
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
    val isFavorite: Boolean = false
)

object AdventureProposalGenerator {

    fun generateProposals(baseItinerary: Itinerary, preferences: TravelPreferences): List<AdventureProposal> {
        val destination = if (baseItinerary.destino.isNotBlank()) baseItinerary.destino else "la ciudad"

        // Sample Side Quests
        val sampleSideQuests = listOf(
            SideQuestItem(
                id = "sq1",
                title = "Hidden Courtyard",
                category = "☕ Patio Escondido",
                description = "Un pequeño patio interno rodeado de jazmines con mesitas de madera.",
                distanceDetourText = "A 3 min de tu ruta",
                iconEmoji = "🌿",
                latitud = -34.582,
                longitud = -58.419,
                walkingTimeText = "3 min",
                imageUrl = "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?q=80&w=1000&auto=format&fit=crop"
            ),
            SideQuestItem(
                id = "sq2",
                title = "Mural de Autor Oculto",
                category = "🎨 Street Art",
                description = "Obra secreta pintada por un artista urbano internacional.",
                distanceDetourText = "A 2 min",
                iconEmoji = "🎨",
                latitud = -34.584,
                longitud = -58.421,
                walkingTimeText = "2 min",
                imageUrl = "https://images.unsplash.com/photo-1561055657-b9e0bf0fa360?q=80&w=1000&auto=format&fit=crop"
            ),
            SideQuestItem(
                id = "sq3",
                title = "Librería de Anticuario",
                category = "📚 Rincón Literario",
                description = "Ediciones raras y aroma a papel antiguo en una bóveda silenciosa.",
                distanceDetourText = "A 4 min",
                iconEmoji = "📖",
                latitud = -34.586,
                longitud = -58.415,
                walkingTimeText = "4 min",
                imageUrl = "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=1000&auto=format&fit=crop"
            )
        )

        // 1. Hidden Gardens (Nature Escape)
        val proposal1 = AdventureProposal(
            id = "hidden_gardens",
            title = "Hidden Gardens",
            emoji = "🌿",
            category = "Nature Escape",
            tagline = "Parques tranquilos, pasajes escondidos y cafés de patio verde en $destination.",
            introNarrative = "Conectate con la naturaleza escondida de $destination.\nUn paseo entre jardines históricos, plazas tranquilas y pequeños cafés donde el tiempo parece detenerse.",
            durationText = "3.5 h",
            distanceText = "3.2 km",
            difficulty = "Relaxed",
            atmosphere = "Serena · Verde · Aire Libre",
            highlights = listOf("Pasajes Botánicos", "Café de Patio", "Senderos Arbolados"),
            mainQuestStops = listOf(
                ItineraryStop(
                    order = 1,
                    type = StopType.START,
                    visualType = ActivityVisualType.NATURE,
                    horaInicio = "10:00 AM",
                    titulo = "Jardín Botánico Carlos Thays",
                    descripcion = "Un oasis de calma con invernaderos de hierro del siglo XIX y esculturas centenarias.",
                    duracionEstimada = "1h 15m",
                    costoEstimado = "Gratis",
                    distanceFromPrevious = "Punto de inicio",
                    historicalInfo = "Diseñado en 1898 por el paisajista francés Carlos Thays.",
                    openingHours = "Mar - Dom: 09:00 - 18:30 hs",
                    funFact = "El invernadero principal fue traído desarmado desde París.",
                    audioGuideDuration = "3 min audio",
                    imageUrl = "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1000&auto=format&fit=crop"
                ),
                ItineraryStop(
                    order = 2,
                    type = StopType.PLACE,
                    visualType = ActivityVisualType.CULTURE,
                    horaInicio = "11:20 AM",
                    titulo = "Pasaje Russell & Faroles",
                    descripcion = "Un secreto entre paredes de ladrillo visto y faroles victorianos.",
                    duracionEstimada = "45m",
                    costoEstimado = "Gratis",
                    distanceFromPrevious = "A 450m del Botánico",
                    historicalInfo = "Pasaje urbano pintoresco conservado desde 1900.",
                    openingHours = "Acceso libre 24 hs",
                    funFact = "Muchos murales cambian periódicamente.",
                    audioGuideDuration = "2 min audio",
                    imageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1000&auto=format&fit=crop"
                )
            ),
            sideQuests = sampleSideQuests,
            baseItinerary = baseItinerary,
            imageResId = R.drawable.park_placeholder,
            heroImageUrl = "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?q=80&w=1400&auto=format&fit=crop",
            isFavorite = true
        )

        // 2. Coffee & Corners (Gourmet Walk)
        val proposal2 = AdventureProposal(
            id = "coffee_corners",
            title = "Coffee & Corners",
            emoji = "☕",
            category = "Gourmet Walk",
            tagline = "Librerías de autor, cafés de aroma intenso y calles empedradas para caminar sin prisa.",
            introNarrative = "Sabor, literatura y pausa urbana en $destination.\nUn recorrido pensado para disfrutarse sorbo a sorbo entre casonas históricas y aromas de tostado recién hecho.",
            durationText = "2.5 h",
            distanceText = "1.9 km",
            difficulty = "Calma",
            atmosphere = "Acogedora · Bohemia · Cálida",
            highlights = listOf("Cafetería de Especialidad", "Pasajes Históricos", "Librería Clásica"),
            mainQuestStops = listOf(
                ItineraryStop(
                    order = 1,
                    type = StopType.FOOD,
                    visualType = ActivityVisualType.FOOD,
                    horaInicio = "10:30 AM",
                    titulo = "Café de Casona Reciclada",
                    descripcion = "Brunch artesanal bajo una parra centenaria en una residencia de 1920.",
                    duracionEstimada = "1h",
                    costoEstimado = "$$",
                    distanceFromPrevious = "Punto de inicio",
                    historicalInfo = "Construida en 1920 como casona familiar.",
                    openingHours = "Mié - Dom: 09:00 - 20:00 hs",
                    funFact = "Sirven café de especialidad de fincas seleccionadas.",
                    audioGuideDuration = "2 min audio",
                    imageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=1000&auto=format&fit=crop"
                )
            ),
            sideQuests = listOf(sampleSideQuests[0], sampleSideQuests[2]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.cafe_placeholder,
            heroImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=1400&auto=format&fit=crop"
        )

        // 3. Urban Explorer (Urban Adventure)
        val proposal3 = AdventureProposal(
            id = "urban_explorer",
            title = "Urban Explorer",
            emoji = "🔥",
            category = "Urban Adventure",
            tagline = "Street art, pasajes alternativos y rincones vibrantes de la escena local.",
            introNarrative = "Descubrí la energía creativa palpitante de $destination.\nDesde intervenciones de arte urbano hasta callejones de ladrillo y galerías independientes.",
            durationText = "4.0 h",
            distanceText = "4.2 km",
            difficulty = "Exploratoria",
            atmosphere = "Vibrante · Moderna · Creativa",
            highlights = listOf("Graffitis Famosos", "Arquitectura Local", "Mercados Urbanos"),
            mainQuestStops = listOf(
                ItineraryStop(
                    order = 1,
                    type = StopType.PLACE,
                    visualType = ActivityVisualType.PHOTO,
                    horaInicio = "11:00 AM",
                    titulo = "Murales de Arte Urbano",
                    descripcion = "Galería al aire libre con murales de gran formato de artistas de renombre.",
                    duracionEstimada = "1h",
                    costoEstimado = "Gratis",
                    distanceFromPrevious = "Punto de inicio",
                    imageUrl = "https://images.unsplash.com/photo-1561055657-b9e0bf0fa360?q=80&w=1000&auto=format&fit=crop"
                )
            ),
            sideQuests = listOf(sampleSideQuests[1], sampleSideQuests[0]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.streetart_placeholder,
            heroImageUrl = "https://images.unsplash.com/photo-1561055657-b9e0bf0fa360?q=80&w=1400&auto=format&fit=crop"
        )

        // 4. Golden Hour Escape (Sunset Walk)
        val proposal4 = AdventureProposal(
            id = "golden_hour",
            title = "Golden Hour Escape",
            emoji = "🌅",
            category = "Sunset Walk",
            tagline = "Ruta diseñada para culminar en un mirador ideal justo durante la puesta de sol.",
            introNarrative = "La dorada luz de la tarde transformando cada fachada de $destination.\nRutas abiertas que convergen en terrazas mirador antes de que caiga la noche.",
            durationText = "3.0 h",
            distanceText = "2.5 km",
            difficulty = "Mágica",
            atmosphere = "Atardecer · Vista Abierta · Fotogénica",
            highlights = listOf("Ruta del Sol", "Mirador Elevado", "Brindis al Atardecer"),
            mainQuestStops = listOf(
                ItineraryStop(
                    order = 1,
                    type = StopType.PLACE,
                    visualType = ActivityVisualType.PHOTO,
                    horaInicio = "05:00 PM",
                    titulo = "Mirador Panorámico del Lago",
                    descripcion = "Vistas despejadas hacia el horizonte para capturar la hora dorada perfecta.",
                    duracionEstimada = "1h",
                    costoEstimado = "Gratis",
                    distanceFromPrevious = "Punto de inicio",
                    imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1000&auto=format&fit=crop"
                )
            ),
            sideQuests = sampleSideQuests,
            baseItinerary = baseItinerary,
            imageResId = R.drawable.sunset_placeholder,
            heroImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop"
        )

        // 5. Historic Journey (Classic Landmarks)
        val proposal5 = AdventureProposal(
            id = "historic_journey",
            title = "Historic Journey",
            emoji = "🏛",
            category = "Classic Landmarks",
            tagline = "Los íconos imprescindibles y la esencia histórica de $destination.",
            introNarrative = "Sumergite en la historia viva de $destination.\nMonumentos icónicos, palacios señoriales y plazas que atesoran más de dos siglos de memoria.",
            durationText = "3.2 h",
            distanceText = "3.1 km",
            difficulty = "Equilibrada",
            atmosphere = "Monumental · Clásica · Emblemática",
            highlights = listOf("Monumentos Clásicos", "Plazas Principales", "Arquitectura Neoclásica"),
            mainQuestStops = listOf(
                ItineraryStop(
                    order = 1,
                    type = StopType.PLACE,
                    visualType = ActivityVisualType.HISTORY,
                    horaInicio = "09:30 AM",
                    titulo = "Plaza de Mayo & Cabildo",
                    descripcion = "Epicentro histórico de la vida política y social de la ciudad.",
                    duracionEstimada = "1h 15m",
                    costoEstimado = "Gratis",
                    distanceFromPrevious = "Punto de inicio",
                    imageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1000&auto=format&fit=crop"
                )
            ),
            sideQuests = listOf(sampleSideQuests[0], sampleSideQuests[2]),
            baseItinerary = baseItinerary,
            imageResId = R.drawable.landmarks_placeholder,
            heroImageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1400&auto=format&fit=crop"
        )

        return listOf(proposal1, proposal2, proposal3, proposal4, proposal5)
    }
}
