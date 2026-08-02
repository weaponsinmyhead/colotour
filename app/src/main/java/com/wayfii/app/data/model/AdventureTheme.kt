package com.wayfii.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AdventureTheme(
    val id: String,
    val title: String,
    val mood: String,
    val colorPalette: AdventureColorPalette,
    val heroPhotographyStyle: String,
    val narrativeTone: String,
    val iconEmoji: String,
    val timelineStyle: String,
    val badgeStyle: String,
    val sideQuestStyle: String,
    val mapAccentColorHex: String,
    val completionBadgeTitle: String
) {
    companion object {
        val HIDDEN_GARDENS = AdventureTheme(
            id = "hidden_gardens",
            title = "🌿 Hidden Gardens",
            mood = "Serena · Verde · Aire Libre",
            colorPalette = AdventureColorPalette("#00897B", "#00BFA5", "#E0F2F1", "#F4FBF7"),
            heroPhotographyStyle = "Verde radiante, luz filtrada entre hojas y flores en primer plano",
            narrativeTone = "Relajada, contemplativa y natural",
            iconEmoji = "🌿",
            timelineStyle = "Senderos botánicos",
            badgeStyle = "Verde Primavera",
            sideQuestStyle = "Patio Escondido",
            mapAccentColorHex = "#00897B",
            completionBadgeTitle = "Explorador Botánico"
        )

        val COFFEE_CORNERS = AdventureTheme(
            id = "coffee_corners",
            title = "☕ Coffee & Corners",
            mood = "Acogedora · Bohemia · Cálida",
            colorPalette = AdventureColorPalette("#78350F", "#92400E", "#FEF3C7", "#FFFDF5"),
            heroPhotographyStyle = "Detalle de café de especialidad, aroma a pastelería y mesas de madera",
            narrativeTone = "Acogedora, detallista y pausada",
            iconEmoji = "☕",
            timelineStyle = "Pasajes & Cafés",
            badgeStyle = "Ámbar Cálido",
            sideQuestStyle = "Rincón de Autor",
            mapAccentColorHex = "#92400E",
            completionBadgeTitle = "Gourmet Barrial"
        )

        val URBAN_EXPLORER = AdventureTheme(
            id = "urban_explorer",
            title = "🔥 Urban Explorer",
            mood = "Vibrante · Moderna · Creativa",
            colorPalette = AdventureColorPalette("#E11D48", "#BE123C", "#FFE4E6", "#FFF1F2"),
            heroPhotographyStyle = "Murales coloridos, arquitectura alternativa y encuadres dinámicos",
            narrativeTone = "Energética, curiosa y vanguardista",
            iconEmoji = "🔥",
            timelineStyle = "Circuito de Arte Urbano",
            badgeStyle = "Rosa Neón",
            sideQuestStyle = "Mural Oculto",
            mapAccentColorHex = "#E11D48",
            completionBadgeTitle = "Urban Explorer"
        )

        val GOLDEN_HOUR_ESCAPE = AdventureTheme(
            id = "golden_hour",
            title = "🌅 Golden Hour Escape",
            mood = "Atardecer · Vista Abierta · Fotogénica",
            colorPalette = AdventureColorPalette("#EA580C", "#C2410C", "#FFEDD5", "#FFF7ED"),
            heroPhotographyStyle = "Cúpulas encendidas por el sol poniente y siluetas al atardecer",
            narrativeTone = "Mágica, poética y fotográfica",
            iconEmoji = "🌅",
            timelineStyle = "Ruta de Miradores",
            badgeStyle = "Naranja Puesta de Sol",
            sideQuestStyle = "Spot de Fotos Atardecer",
            mapAccentColorHex = "#EA580C",
            completionBadgeTitle = "Cazador de Atardeceres"
        )

        val HISTORIC_JOURNEY = AdventureTheme(
            id = "historic_journey",
            title = "🏛 Historic Journey",
            mood = "Monumental · Clásica · Emblemática",
            colorPalette = AdventureColorPalette("#1E3A8A", "#1D4ED8", "#DBEAFE", "#F8FAFC"),
            heroPhotographyStyle = "Columnatas neoclásicas, fachadas señoriales y monumentos",
            narrativeTone = "Elegante, histórica y monumental",
            iconEmoji = "🏛",
            timelineStyle = "Eje Histórico",
            badgeStyle = "Azul Real",
            sideQuestStyle = "Reliquia del Pasado",
            mapAccentColorHex = "#1D4ED8",
            completionBadgeTitle = "Historiador Urbano"
        )

        val BUENOS_AIRES_BLOOM = AdventureTheme(
            id = "ba_in_bloom",
            title = "🌸 Buenos Aires en Flor",
            mood = "Romántica · Floral · Vibrante",
            colorPalette = AdventureColorPalette("#8E24AA", "#AB47BC", "#F3E5F5", "#FAF5FC"),
            heroPhotographyStyle = "Jacarandás púrpuras cubriendo las avenidas y pétalos dorados",
            narrativeTone = "Encantadora, romántica y festiva",
            iconEmoji = "🌸",
            timelineStyle = "Ruta Floral",
            badgeStyle = "Púrpura Jacarandá",
            sideQuestStyle = "Secreto Botánico",
            mapAccentColorHex = "#8E24AA",
            completionBadgeTitle = "Explorador de Flores"
        )

        val AUTUMN_WALK = AdventureTheme(
            id = "autumn_walk",
            title = "🍂 Caminata entre Hojas Doradas",
            mood = "Nostálgica · Cálida · Poética",
            colorPalette = AdventureColorPalette("#D97706", "#B45309", "#FEF3C7", "#FFFBEB"),
            heroPhotographyStyle = "Plátanos amarillos, hojas secas en la vereda y luz de otoño",
            narrativeTone = "Tranquila, literaria y acogedora",
            iconEmoji = "🍂",
            timelineStyle = "Paseo de Verdes y Ámbar",
            badgeStyle = "Cobre Otoñal",
            sideQuestStyle = "Librería Oculta",
            mapAccentColorHex = "#D97706",
            completionBadgeTitle = "Poeta de Otoño"
        )

        val BUENOS_AIRES_AFTER_DARK = AdventureTheme(
            id = "ba_after_dark",
            title = "🌙 Buenos Aires After Dark",
            mood = "Nocturna · Sofisticada · Mágica",
            colorPalette = AdventureColorPalette("#4F46E5", "#3730A3", "#E0E7FF", "#EEF2FF"),
            heroPhotographyStyle = "Faroles victorianos encendidos, barras de jazz y calles iluminadas",
            narrativeTone = "Sofisticada, nocturna e intrépida",
            iconEmoji = "🌙",
            timelineStyle = "Ruta Speakeasy & Jazz",
            badgeStyle = "Índigo Nocturno",
            sideQuestStyle = "Barra Secreta",
            mapAccentColorHex = "#4F46E5",
            completionBadgeTitle = "Noctámbulo Urbano"
        )

        val ALL_THEMES = listOf(
            HIDDEN_GARDENS,
            COFFEE_CORNERS,
            URBAN_EXPLORER,
            GOLDEN_HOUR_ESCAPE,
            HISTORIC_JOURNEY,
            BUENOS_AIRES_BLOOM,
            AUTUMN_WALK,
            BUENOS_AIRES_AFTER_DARK
        )
    }
}
