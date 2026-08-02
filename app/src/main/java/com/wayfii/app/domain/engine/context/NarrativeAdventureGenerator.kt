package com.wayfii.app.domain.engine.context

import com.wayfii.app.data.model.*

class NarrativeAdventureGenerator {

    fun generateNarratives(
        context: ContextEnvironment,
        preferences: TravelPreferences
    ): List<AdventureDNA> {
        val destination = if (context.cityName.isNotBlank()) context.cityName else "la ciudad"
        val dnaList = mutableListOf<AdventureDNA>()

        // 1. PRIMARY SEASONAL HERO NARRATIVE
        val seasonalDna = when (context.season) {
            Season.SPRING -> AdventureDNA(
                title = "🌸 $destination en Flor",
                mood = "Fresco · Romántico · Floral",
                narrative = "El perfume de los jazmines y la floración de los jacarandás transforman las calles de $destination.\nUn recorrido sereno por rosadales, jardines botánicos y cafés con patios verdes al aire libre.",
                categoryTag = "Paseo Floral",
                heroImageUrl = "https://images.unsplash.com/photo-1522383225653-ed111181a951?q=80&w=1400&auto=format&fit=crop",
                colorPalette = AdventureColorPalette(
                    primaryHex = "#8E24AA",
                    secondaryHex = "#00897B",
                    accentHex = "#E1BEE7",
                    cardBgHex = "#FAF5FC"
                ),
                photographyStyle = "Luz suave matutina, flores desdibujadas en primer plano y verde radiante",
                highlights = listOf("Jacarandás & Flores", "Jardín de las Rosas", "Café de Patio al Aire Libre", "Picnic Urbano"),
                sideQuestTheme = "🌿 Rincón Botánico",
                badgeText = "🌸 Temporada de Floración",
                iconEmoji = "🌸",
                microcopy = "Ideal para recorrer caminando bajo el sol cálido de primavera."
            )

            Season.SUMMER -> AdventureDNA(
                title = "🌳 Oasis a la Sombra & Helados",
                mood = "Fresco · Relajado · Brisa Marina",
                narrative = "Descubrí la frescura escondida de $destination durante los días calurosos.\nSenderos arbolados de gran copa, paradas estratégicas en las mejores heladerías artesanales y brisa de río.",
                categoryTag = "Oasis Urbano",
                heroImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop",
                colorPalette = AdventureColorPalette(
                    primaryHex = "#0284C7",
                    secondaryHex = "#15803D",
                    accentHex = "#BAE6FD",
                    cardBgHex = "#F0F9FF"
                ),
                photographyStyle = "Sombras enérgicas bajo árboles centenarios y reflejos en el agua",
                highlights = listOf("Túnel de Sombra", "Heladería de Autor", "Paseo Costero", "Bebidas Frías"),
                sideQuestTheme = "🍦 Heladería Secreta",
                badgeText = "☀️ Sombra & Brisa",
                iconEmoji = "🌳",
                microcopy = "Aprovechá la sombra de los árboles para capear el calor matutino."
            )

            Season.AUTUMN -> AdventureDNA(
                title = "🍂 Caminata entre Hojas Doradas",
                mood = "Cálido · Nostálgico · Poético",
                narrative = "Las copas de los fresnos y plátanos doran las veredas de $destination.\nCaminá despacio entre alfombras de hojas secas, pequeñas librerías con olor a café y pasajes históricos iluminados por la luz ámbar de la tarde.",
                categoryTag = "Ruta Melancólica",
                heroImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop",
                colorPalette = AdventureColorPalette(
                    primaryHex = "#D97706",
                    secondaryHex = "#B45309",
                    accentHex = "#FEF3C7",
                    cardBgHex = "#FFFBEB"
                ),
                photographyStyle = "Contraluces cálidos, hojas de tono cobrizo y textura de ladrillo antiguo",
                highlights = listOf("Senderos de Hojas Doradas", "Librería de Anticuario", "Café de Especialidad", "Casonas Históricas"),
                sideQuestTheme = "📚 Pasaje Oculto",
                badgeText = "🍂 Hojas Doradas de Otoño",
                iconEmoji = "🍂",
                microcopy = "El sol de otoño crea la iluminación dorada perfecta para fotos."
            )

            Season.WINTER -> AdventureDNA(
                title = "☕ Refugios de Invierno",
                mood = "Acogedor · Cultural · Intimo",
                narrative = "Resguardate del frío en el alma cálida de $destination.\nUna travesía entre cafés notables centenarios, galerías de arte cubiertas, bibliotecas históricas y chocolates calientes en casonas de época.",
                categoryTag = "Experiencia Cálida",
                heroImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=1400&auto=format&fit=crop",
                colorPalette = AdventureColorPalette(
                    primaryHex = "#78350F",
                    secondaryHex = "#92400E",
                    accentHex = "#FDE68A",
                    cardBgHex = "#FFFDF5"
                ),
                photographyStyle = "Luces cálidas de interiores, vapor de café e iluminación envolvente",
                highlights = listOf("Café Notable Histórico", "Pasaje techado", "Museo de Arte", "Chocolate Artesanal"),
                sideQuestTheme = "🏛️ Pasaje Cubierto",
                badgeText = "☕ Refugio de Invierno",
                iconEmoji = "☕",
                microcopy = "Recorrido pensado para permanecer a resguardo del aire frío."
            )
        }
        dnaList.add(seasonalDna)

        // 2. TIME OF DAY / WEATHER SPECIFIC NARRATIVE
        if (context.weather == WeatherCondition.RAIN) {
            dnaList.add(
                AdventureDNA(
                    title = "☔ Recorridos a Buen Resguardo",
                    mood = "Tranquilo · Introspectivo · Resguardado",
                    narrative = "La lluvia resalta el brillo del empedrado y el encanto nostálgico de $destination.\nConectá mercados bajo techo, arcanos literarios, pasajes victorianos y salones de té donde resguardarte con estilo.",
                    categoryTag = "Paseo Bajo Techo",
                    heroImageUrl = "https://images.unsplash.com/photo-1519692933481-e162a57d6721?q=80&w=1400&auto=format&fit=crop",
                    colorPalette = AdventureColorPalette(
                        primaryHex = "#2563EB",
                        secondaryHex = "#1E40AF",
                        accentHex = "#DBEAFE",
                        cardBgHex = "#F8FAFC"
                    ),
                    photographyStyle = "Reflejos sobre el asfalto mojado, paraguas coloridos y ventanas empañadas",
                    highlights = listOf("Pasajes de Arquitectura Techada", "Mercado Gastronómico Cubierto", "Galería de Arte Interna", "Té de Autor"),
                    sideQuestTheme = "☂️ Rincón Techado",
                    badgeText = "☔ Adaptado a Lluvia",
                    iconEmoji = "☔",
                    microcopy = "100% libre de caminatas descubiertas bajo el agua."
                )
            )
        } else if (context.timeOfDay == TimeOfDay.GOLDEN_HOUR) {
            dnaList.add(
                AdventureDNA(
                    title = "🌅 La Ciudad Dorada",
                    mood = "Fotogénico · Mágico · Vibrante",
                    narrative = "Experimentá los minutos mágicos donde el sol poniente enciende las cúpulas de $destination.\nUna ruta ascendente que culmina en terrazas mirador y paseos ribereños para despedir el día.",
                    categoryTag = "Atardecer & Miradores",
                    heroImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=1400&auto=format&fit=crop",
                    colorPalette = AdventureColorPalette(
                        primaryHex = "#EA580C",
                        secondaryHex = "#C2410C",
                        accentHex = "#FFEDD5",
                        cardBgHex = "#FFF7ED"
                    ),
                    photographyStyle = "Siluetas al atardecer, cúpulas doradas e iluminación cálida de horizonte",
                    highlights = listOf("Mirador Elevado Panorámico", "Paseo del Río", "Cúpulas Iluminadas", "Brindis al Atardecer"),
                    sideQuestTheme = "🌇 Spot de Puesta de Sol",
                    badgeText = "🌅 Hora Dorada Especial",
                    iconEmoji = "🌅",
                    microcopy = "Planificado cronométricamente para llegar al mirador al atardecer."
                )
            )
        } else if (context.timeOfDay == TimeOfDay.NIGHT) {
            dnaList.add(
                AdventureDNA(
                    title = "🌙 $destination After Dark",
                    mood = "Sofisticado · Nocturno · Vibrante",
                    narrative = "Cuando cae el sol, la arquitectura encendida y la vida nocturna despiertan en $destination.\nSumergite en bares ocultos speakeasy, clubes de jazz tenue, comida callejera y avenidas iluminadas.",
                    categoryTag = "Aventura Nocturna",
                    heroImageUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?q=80&w=1400&auto=format&fit=crop",
                    colorPalette = AdventureColorPalette(
                        primaryHex = "#4F46E5",
                        secondaryHex = "#3730A3",
                        accentHex = "#E0E7FF",
                        cardBgHex = "#EEF2FF"
                    ),
                    photographyStyle = "Luces de neón, faroles victorianos e iluminación nocturna de arquitectura",
                    highlights = listOf("Bar Speakeasy Secreto", "Club de Jazz en Vivo", "Monumentos Iluminados", "Cocktails de Autor"),
                    sideQuestTheme = "🎷 Barra Oculta",
                    badgeText = "🌙 Experiencia Nocturna",
                    iconEmoji = "🌙",
                    microcopy = "Disfrutá la atmósfera elegante y misteriosa de la noche."
                )
            )
        } else {
            // Afternoon default light
            dnaList.add(
                AdventureDNA(
                    title = "📷 Luces & Arquitectura Urbana",
                    mood = "Urbano · Detallista · Estético",
                    narrative = "Descubrí la combinación entre casonas históricas, pasajes con historia y diseño contemporáneo en $destination.",
                    categoryTag = "Paseo Fotográfico",
                    heroImageUrl = "https://images.unsplash.com/photo-1513694203232-719a280e022f?q=80&w=1400&auto=format&fit=crop",
                    colorPalette = AdventureColorPalette(
                        primaryHex = "#0D9488",
                        secondaryHex = "#0F766E",
                        accentHex = "#CCFBF1",
                        cardBgHex = "#F0FDF4"
                    ),
                    photographyStyle = "Enfoque arquitectónico, detalles de fachada y perspectiva limpia",
                    highlights = listOf("Pasajes de Época", "Murales de Autor", "Librerías de Fachada", "Miradores Urbanos"),
                    sideQuestTheme = "📸 Photo Spot Oculto",
                    badgeText = "📷 Especial Fotografía",
                    iconEmoji = "🏛️",
                    microcopy = "Ideal para amantes de la arquitectura y la fotografía urbana."
                )
            )
        }

        // 3. ESSENTIAL CLASSICS / GOURMET EXPERIENCE
        dnaList.add(
            AdventureDNA(
                title = "☕ Esencia & Aromas Barriales",
                mood = "Calmo · Auténtico · Gastronómico",
                narrative = "Los clásicos que nunca fallan adaptados al horario de hoy.\nDisfrutá cafés de origen, panaderías de masa madre y calles con arbolado histórico.",
                categoryTag = "Ruta Gastronómica",
                heroImageUrl = "https://images.unsplash.com/photo-1554118811-1e0d58224f24?q=80&w=1400&auto=format&fit=crop",
                colorPalette = AdventureColorPalette(
                    primaryHex = "#059669",
                    secondaryHex = "#047857",
                    accentHex = "#D1FAE5",
                    cardBgHex = "#ECFDF5"
                ),
                photographyStyle = "Planos detalle de café y pastelería con fondos cálidos",
                highlights = listOf("Café de Especialidad", "Panadería Artesanal", "Plaza de Barrio", "Mercado Local"),
                sideQuestTheme = "🥐 Rincón Dulce",
                badgeText = "☕ Gourmet & Pausa",
                iconEmoji = "🥐",
                microcopy = "Ideal para tomarse una pausa y disfrutar despacio."
            )
        )

        // 4. URBAN EXPLORER
        dnaList.add(
            AdventureDNA(
                title = "🎨 Esquina Creativa & Street Art",
                mood = "Bohemio · Alternativo · Vibrante",
                narrative = "Galerías independientes, intervenciones urbanas y pasajes con murales deslumbrantes en $destination.",
                categoryTag = "Arte & Vanguardia",
                heroImageUrl = "https://images.unsplash.com/photo-1561055657-b9e0bf0fa360?q=80&w=1400&auto=format&fit=crop",
                colorPalette = AdventureColorPalette(
                    primaryHex = "#E11D48",
                    secondaryHex = "#BE123C",
                    accentHex = "#FFE4E6",
                    cardBgHex = "#FFF1F2"
                ),
                photographyStyle = "Colores vivos, contraste de murales y perspectiva callejera",
                highlights = listOf("Murales de Artistas Internacionales", "Tienda de Diseño", "Galería Independiente", "Pasaje Pintoresco"),
                sideQuestTheme = "🎨 Mural Oculto",
                badgeText = "🎨 Escena Bohemia",
                iconEmoji = "🎨",
                microcopy = "Descubrí el lado más fresco e independiente de la ciudad."
            )
        )

        return dnaList
    }
}
