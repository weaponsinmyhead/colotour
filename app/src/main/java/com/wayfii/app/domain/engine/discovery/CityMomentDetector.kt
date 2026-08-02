package com.wayfii.app.domain.engine.discovery

import com.wayfii.app.data.model.*

class CityMomentDetector {

    private val allMoments = listOf(
        CityMoment(
            id = "jacaranda_bloom",
            name = "Floración de Jacarandás",
            description = "Las avenidas y plazas de Buenos Aires se tiñen de púrpura intenso.",
            season = Season.SPRING,
            activeMonths = listOf(10, 11),
            priority = 95,
            contextualReasonLabel = "🌸 Solo durante la floración de Jacarandás",
            suggestedThemeIds = listOf("ba_in_bloom", "hidden_gardens"),
            narrativeTone = "Romántica y primaveral",
            heroKeywords = listOf("jacaranda", "purple trees", "flowers"),
            colorPalette = AdventureColorPalette("#8E24AA", "#AB47BC", "#F3E5F5", "#FAF5FC"),
            recommendedTimeOfDay = TimeOfDay.AFTERNOON
        ),
        CityMoment(
            id = "rose_garden_peak",
            name = "Pico de Floración del Rosedal",
            description = "Más de 18.000 rosales en su máxima expresión de color y perfume.",
            season = Season.SPRING,
            activeMonths = listOf(10, 11, 12),
            priority = 90,
            contextualReasonLabel = "🌹 Pico de floración del Rosedal de Palermo",
            suggestedThemeIds = listOf("hidden_gardens", "ba_in_bloom"),
            narrativeTone = "Floral y contemplativa",
            heroKeywords = listOf("roses", "rose garden", "palermo"),
            colorPalette = AdventureColorPalette("#D81B60", "#F48FB1", "#FCE4EC", "#FFF8FA"),
            recommendedTimeOfDay = TimeOfDay.MORNING
        ),
        CityMoment(
            id = "golden_tree_season",
            name = "Temporada de Hojas Doradas",
            description = "Fresnos y plátanos dorados cubren las veredas antiguas de la ciudad.",
            season = Season.AUTUMN,
            activeMonths = listOf(4, 5),
            priority = 92,
            contextualReasonLabel = "🍂 Exclusivo de la temporada de hojas doradas",
            suggestedThemeIds = listOf("autumn_walk", "coffee_corners"),
            narrativeTone = "Nostálgica y cálida",
            heroKeywords = listOf("autumn trees", "golden leaves", "cobblestone"),
            colorPalette = AdventureColorPalette("#D97706", "#B45309", "#FEF3C7", "#FFFBEB"),
            recommendedTimeOfDay = TimeOfDay.GOLDEN_HOUR
        ),
        CityMoment(
            id = "rooftop_season",
            name = "Atardeceres en Terrazas & Rooftops",
            description = "Vistas despejadas y cocktails al sol poniente sobre cúpulas históricas.",
            season = Season.SUMMER,
            activeMonths = listOf(12, 1, 2, 3),
            priority = 88,
            contextualReasonLabel = "🌅 Imperdible al atardecer sobre terrazas",
            suggestedThemeIds = listOf("golden_hour", "ba_after_dark"),
            narrativeTone = "Fotogénica y sofisticada",
            heroKeywords = listOf("rooftop", "sunset skyline", "cocktails"),
            colorPalette = AdventureColorPalette("#EA580C", "#C2410C", "#FFEDD5", "#FFF7ED"),
            recommendedTimeOfDay = TimeOfDay.GOLDEN_HOUR
        ),
        CityMoment(
            id = "historic_cafe_season",
            name = "Ruta de Cafés Notables & Librerías",
            description = "Resguardo en casonas de 1900 con aroma a café recién tostado.",
            season = Season.WINTER,
            activeMonths = listOf(6, 7, 8),
            priority = 85,
            contextualReasonLabel = "☕ Refugio perfecto en días fríos de café e historia",
            suggestedThemeIds = listOf("coffee_corners", "historic_journey"),
            narrativeTone = "Acogedora e íntima",
            heroKeywords = listOf("historic cafe", "cozy coffee", "bookstore"),
            colorPalette = AdventureColorPalette("#78350F", "#92400E", "#FEF3C7", "#FFFDF5"),
            recommendedTimeOfDay = TimeOfDay.AFTERNOON
        ),
        CityMoment(
            id = "rainy_museum_day",
            name = "Circuitos a Buen Resguardo",
            description = "Galerías cubiertas, pasajes techados y salones de arte sin mojarse.",
            season = null,
            priority = 95, // High priority when raining
            contextualReasonLabel = "☔ 100% resguardado bajo techo ideal para lluvia",
            suggestedThemeIds = listOf("historic_journey", "coffee_corners"),
            narrativeTone = "Tranquila y resguardada",
            heroKeywords = listOf("museum gallery", "covered arcade", "tea room"),
            colorPalette = AdventureColorPalette("#2563EB", "#1E40AF", "#DBEAFE", "#F8FAFC"),
            recommendedTimeOfDay = null
        ),
        CityMoment(
            id = "san_telmo_market_day",
            name = "Feria Tradicional de San Telmo",
            description = "Antigüedades, artistas callejeros y tango en empedrados centenarios.",
            season = null,
            activeDaysOfWeek = listOf("Domingo"),
            isSpecialEvent = true,
            priority = 98,
            contextualReasonLabel = "🎭 Disponible únicamente los domingos en San Telmo",
            suggestedThemeIds = listOf("urban_explorer", "historic_journey"),
            narrativeTone = "Bohemia y festiva",
            heroKeywords = listOf("san telmo market", "antiques", "cobblestone street"),
            colorPalette = AdventureColorPalette("#E11D48", "#BE123C", "#FFE4E6", "#FFF1F2"),
            recommendedTimeOfDay = TimeOfDay.AFTERNOON
        )
    )

    fun detectActiveMoments(context: ContextEnvironment): List<CityMoment> {
        val active = mutableListOf<CityMoment>()

        for (moment in allMoments) {
            var matches = true

            // Rain override
            if (moment.id == "rainy_museum_day") {
                if (context.weather == WeatherCondition.RAIN) {
                    active.add(moment)
                }
                continue
            }

            // Season match
            if (moment.season != null && moment.season != context.season) {
                matches = false
            }

            // Month match
            if (moment.activeMonths.isNotEmpty() && !moment.activeMonths.contains(context.month)) {
                matches = false
            }

            // Day of week match
            if (moment.activeDaysOfWeek.isNotEmpty() && !moment.activeDaysOfWeek.contains(context.dayOfWeek)) {
                matches = false
            }

            if (matches) {
                active.add(moment)
            }
        }

        return active.sortedByDescending { it.priority }
    }
}
