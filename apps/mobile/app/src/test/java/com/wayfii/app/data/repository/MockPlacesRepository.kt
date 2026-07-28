package com.wayfii.app.data.repository

import com.wayfii.app.data.model.TourismInterest
import com.wayfii.app.data.model.BudgetLevel
import com.wayfii.app.domain.engine.CandidatePlace
import kotlin.math.abs

// Datos deterministas exclusivos de pruebas; no se incluyen en el APK.
class MockPlacesRepository : PlacesRepository {
    override suspend fun getCandidatePlaces(destino: String, baseLat: Double, baseLon: Double): List<CandidatePlace> {
        return listOf(
            CandidatePlace(
                id = "p1",
                nombre = "Café Céntrico Histórico",
                descripcion = "Un rincón antiguo con café excelente y pastelería tradicional.",
                latitud = baseLat + 0.002,
                longitud = baseLon - 0.003,
                estilo = TourismInterest.GASTRONOMICO,
                presupuesto = BudgetLevel.BAJO,
                duracionRecomendadaMinutos = 45,
                costoBasePorPersona = 5.0,
                popularidad = 0.8
            ),
            CandidatePlace(
                id = "p2",
                nombre = "Restaurante Parrilla Criolla",
                descripcion = "Las mejores carnes asadas y platos típicos de la región.",
                latitud = baseLat - 0.004,
                longitud = baseLon + 0.005,
                estilo = TourismInterest.GASTRONOMICO,
                presupuesto = BudgetLevel.MEDIO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 20.0,
                popularidad = 0.9
            ),
            CandidatePlace(
                id = "p3",
                nombre = "Bistró Gourmet de Autor",
                descripcion = "Fusión culinaria de alta cocina en un ambiente elegante.",
                latitud = baseLat + 0.006,
                longitud = baseLon - 0.001,
                estilo = TourismInterest.GASTRONOMICO,
                presupuesto = BudgetLevel.ALTO,
                duracionRecomendadaMinutos = 120,
                costoBasePorPersona = 50.0,
                popularidad = 0.85
            ),
            CandidatePlace(
                id = "p4",
                nombre = "Museo de Historia Nacional",
                descripcion = "Exposiciones históricas permanentes y reliquias de la independencia.",
                latitud = baseLat - 0.002,
                longitud = baseLon - 0.004,
                estilo = TourismInterest.HISTORIA,
                presupuesto = BudgetLevel.BAJO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 3.0,
                popularidad = 0.75
            ),
            CandidatePlace(
                id = "p5",
                nombre = "Teatro Colón o Palacio de las Artes",
                descripcion = "Majestuoso monumento arquitectónico con visitas guiadas y ópera.",
                latitud = baseLat + 0.001,
                longitud = baseLon + 0.003,
                estilo = TourismInterest.CULTURAL,
                presupuesto = BudgetLevel.MEDIO,
                duracionRecomendadaMinutos = 75,
                costoBasePorPersona = 12.0,
                popularidad = 0.95
            ),
            CandidatePlace(
                id = "p6",
                nombre = "Galería de Arte Moderno",
                descripcion = "Obras de artistas vanguardistas nacionales y muestras temporarias.",
                latitud = baseLat - 0.005,
                longitud = baseLon - 0.002,
                estilo = TourismInterest.CULTURAL,
                presupuesto = BudgetLevel.ALTO,
                duracionRecomendadaMinutos = 80,
                costoBasePorPersona = 30.0,
                popularidad = 0.7
            ),
            CandidatePlace(
                id = "p7",
                nombre = "Callejón de Street Art y Graffitis",
                descripcion = "Pasaje colorido lleno de murales pintados por artistas urbanos.",
                latitud = baseLat + 0.005,
                longitud = baseLon + 0.007,
                estilo = TourismInterest.ALTERNATIVO,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 45,
                costoBasePorPersona = 0.0,
                popularidad = 0.8
            ),
            CandidatePlace(
                id = "p8",
                nombre = "Feria de Diseño y Libros Ocultos",
                descripcion = "Artesanías locales, discos vintage y primeras ediciones raras.",
                latitud = baseLat - 0.001,
                longitud = baseLon + 0.004,
                estilo = TourismInterest.ALTERNATIVO,
                presupuesto = BudgetLevel.BAJO,
                duracionRecomendadaMinutos = 60,
                costoBasePorPersona = 8.0,
                popularidad = 0.85
            ),
            CandidatePlace(
                id = "p9",
                nombre = "Club de Jazz Speakeasy",
                descripcion = "Bar oculto con música de jazz en vivo.",
                latitud = baseLat + 0.003,
                longitud = baseLon - 0.005,
                estilo = TourismInterest.ALTERNATIVO,
                presupuesto = BudgetLevel.ALTO,
                duracionRecomendadaMinutos = 100,
                costoBasePorPersona = 35.0,
                popularidad = 0.9
            ),
            CandidatePlace(
                id = "p10",
                nombre = "Parque Recreativo Infantil",
                descripcion = "Espacio verde con juegos interactivos, calesita y lago.",
                latitud = baseLat + 0.004,
                longitud = baseLon + 0.002,
                estilo = TourismInterest.FAMILIAR,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 120,
                costoBasePorPersona = 0.0,
                popularidad = 0.8
            ),
            CandidatePlace(
                id = "p11",
                nombre = "Planetario y Laberinto de Ciencias",
                descripcion = "Proyecciones astronómicas y experimentos divertidos para toda la edades.",
                latitud = baseLat - 0.003,
                longitud = baseLon - 0.003,
                estilo = TourismInterest.FAMILIAR,
                presupuesto = BudgetLevel.MEDIO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 10.0,
                popularidad = 0.88
            ),
            CandidatePlace(
                id = "p12",
                nombre = "Parque de Diversiones",
                descripcion = "Montañas rusas, atracciones y espectáculos familiares.",
                latitud = baseLat + 0.007,
                longitud = baseLon + 0.006,
                estilo = TourismInterest.FAMILIAR,
                presupuesto = BudgetLevel.ALTO,
                duracionRecomendadaMinutos = 180,
                costoBasePorPersona = 40.0,
                popularidad = 0.92
            ),
            CandidatePlace(
                id = "p13",
                nombre = "Monumento Histórico Plaza Central",
                descripcion = "Escultura emblemática en la plaza principal de la ciudad.",
                latitud = baseLat,
                longitud = baseLon,
                estilo = TourismInterest.CLASICO,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 20,
                costoBasePorPersona = 0.0,
                popularidad = 0.85
            ),
            CandidatePlace(
                id = "p14",
                nombre = "Mirador Panorámico del Faro",
                descripcion = "Acceso a la torre más alta con vistas increíbles de 360 grados.",
                latitud = baseLat - 0.002,
                longitud = baseLon + 0.002,
                estilo = TourismInterest.CLASICO,
                presupuesto = BudgetLevel.MEDIO,
                duracionRecomendadaMinutos = 40,
                costoBasePorPersona = 8.0,
                popularidad = 0.9
            ),
            CandidatePlace(
                id = "p15",
                nombre = "Paseo de Compras Tradicional",
                descripcion = "Galería histórica de tiendas finas y recuerdos artesanales.",
                latitud = baseLat + 0.002,
                longitud = baseLon + 0.004,
                estilo = TourismInterest.COMPRAS,
                presupuesto = BudgetLevel.ALTO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 25.0,
                popularidad = 0.78
            ),
            CandidatePlace(
                id = "p16",
                nombre = "Plaza de los Artesanos",
                descripcion = "Feria callejera emblemática los fines de semana.",
                latitud = baseLat + 0.001,
                longitud = baseLon - 0.002,
                estilo = TourismInterest.MAINSTREAM,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 50,
                costoBasePorPersona = 0.0,
                popularidad = 0.9
            ),
            CandidatePlace(
                id = "p17",
                nombre = "Centro de Entretenimiento Urbano",
                descripcion = "Cines, salas de juegos y patio de comidas en la avenida principal.",
                latitud = baseLat - 0.001,
                longitud = baseLon - 0.001,
                estilo = TourismInterest.MAINSTREAM,
                presupuesto = BudgetLevel.MEDIO,
                duracionRecomendadaMinutos = 80,
                costoBasePorPersona = 15.0,
                popularidad = 0.85
            ),
            CandidatePlace(
                id = "p18",
                nombre = "Reserva Natural y Senderos",
                descripcion = "Caminata entre flora y fauna autóctona en un entorno protegido.",
                latitud = baseLat - 0.007,
                longitud = baseLon + 0.008,
                estilo = TourismInterest.NATURALEZA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 110,
                costoBasePorPersona = 0.0,
                popularidad = 0.9
            ),
            CandidatePlace(
                id = "p19",
                nombre = "Mirador del Cerro Silencioso",
                descripcion = "Sendero de trekking que lleva a un balcón natural sobre la ciudad.",
                latitud = baseLat - 0.009,
                longitud = baseLon + 0.009,
                estilo = TourismInterest.NATURALEZA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 120,
                costoBasePorPersona = 0.0,
                popularidad = 0.88
            ),
            CandidatePlace(
                id = "p20",
                nombre = "Puente Colgante para Fotos",
                descripcion = "Punto ideal con la mejor perspectiva del atardecer sobre el río.",
                latitud = baseLat - 0.003,
                longitud = baseLon + 0.006,
                estilo = TourismInterest.FOTOGRAFIA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 30,
                costoBasePorPersona = 0.0,
                popularidad = 0.94
            ),
            CandidatePlace(
                id = "p21",
                nombre = "Anfiteatro y Música al Aire Libre",
                descripcion = "Conciertos gratuitos los fines de semana y espectáculos de danza.",
                latitud = baseLat + 0.008,
                longitud = baseLon - 0.004,
                estilo = TourismInterest.EVENTOS,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 0.0,
                popularidad = 0.86
            ),
            CandidatePlace(
                id = "p22",
                nombre = "Centro de Compras Outlet",
                descripcion = "Tiendas de grandes marcas con descuentos y liquidación de temporada.",
                latitud = baseLat + 0.005,
                longitud = baseLon + 0.009,
                estilo = TourismInterest.COMPRAS,
                presupuesto = BudgetLevel.MEDIO,
                duracionRecomendadaMinutos = 100,
                costoBasePorPersona = 5.0,
                popularidad = 0.8
            ),
            CandidatePlace(
                id = "p23",
                nombre = "Sendero de Trekking Panorámico",
                descripcion = "Ruta de senderismo de dificultad media con vistas panorámicas increíbles.",
                latitud = baseLat - 0.007,
                longitud = baseLon - 0.008,
                estilo = TourismInterest.AVENTURA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 120,
                costoBasePorPersona = 0.0,
                popularidad = 0.9
            ),
            CandidatePlace(
                id = "p24",
                nombre = "Circuito de Caminata Urbana",
                descripcion = "Paseo autoguiado peatonal a través de los rincones más icónicos de la ciudad.",
                latitud = baseLat + 0.002,
                longitud = baseLon + 0.004,
                estilo = TourismInterest.AVENTURA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 0.0,
                popularidad = 0.8
            ),
            CandidatePlace(
                id = "p25",
                nombre = "Reserva Natural con Pasarelas",
                descripcion = "Reserva ecológica protegida ideal para avistaje de aves y contacto con el verde.",
                latitud = baseLat - 0.008,
                longitud = baseLon - 0.005,
                estilo = TourismInterest.NATURALEZA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 90,
                costoBasePorPersona = 0.0,
                popularidad = 0.95
            ),
            CandidatePlace(
                id = "p26",
                nombre = "Mirador del Cerro",
                descripcion = "Punto más alto de la región con vistas espectaculares y binoculares de libre acceso.",
                latitud = baseLat - 0.009,
                longitud = baseLon - 0.009,
                estilo = TourismInterest.AVENTURA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 60,
                costoBasePorPersona = 0.0,
                popularidad = 0.89
            ),
            CandidatePlace(
                id = "p27",
                nombre = "Costanera o Rambla para caminar",
                descripcion = "Camino peatonal pavimentado a orillas del agua, ideal para relajarse y respirar aire fresco.",
                latitud = baseLat - 0.001,
                longitud = baseLon + 0.007,
                estilo = TourismInterest.NATURALEZA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 60,
                costoBasePorPersona = 0.0,
                popularidad = 0.85
            ),
            CandidatePlace(
                id = "p28",
                nombre = "Parque Público con Lago",
                descripcion = "Extenso espacio verde municipal con senderos peatonales y alquiler de botes.",
                latitud = baseLat + 0.003,
                longitud = baseLon - 0.007,
                estilo = TourismInterest.NATURALEZA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 80,
                costoBasePorPersona = 0.0,
                popularidad = 0.91
            ),
            CandidatePlace(
                id = "p29",
                nombre = "Circuito de Bicicleta Recreativa",
                descripcion = "Ciclopista segura y panorámica que conecta múltiples parques de la zona.",
                latitud = baseLat + 0.004,
                longitud = baseLon + 0.003,
                estilo = TourismInterest.AVENTURA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 100,
                costoBasePorPersona = 0.0,
                popularidad = 0.87
            ),
            CandidatePlace(
                id = "p30",
                nombre = "Playa o Balneario Público",
                descripcion = "Zona de arena y agua apta para baño o picnic con servicios básicos gratuitos.",
                latitud = baseLat - 0.006,
                longitud = baseLon + 0.008,
                estilo = TourismInterest.NATURALEZA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 120,
                costoBasePorPersona = 0.0,
                popularidad = 0.92
            ),
            CandidatePlace(
                id = "p31",
                nombre = "Punto Fotográfico al Atardecer",
                descripcion = "Pequeño mirador y plataforma ideal para fotografiar la puesta de sol.",
                latitud = baseLat - 0.004,
                longitud = baseLon + 0.007,
                estilo = TourismInterest.FOTOGRAFIA,
                presupuesto = BudgetLevel.GRATUITO,
                duracionRecomendadaMinutos = 30,
                costoBasePorPersona = 0.0,
                popularidad = 0.93
            )
        )
    }
}
