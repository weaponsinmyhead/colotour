package com.colotour.app.domain.engine

import com.colotour.app.data.model.*
import com.colotour.app.data.repository.GeocodingRepository
import com.colotour.app.data.repository.MockPlacesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryEngineTest {

    private val placesRepository = MockPlacesRepository()
    private val fakeGeocodingRepository = FakeGeocodingRepository()
    private val engine = ItineraryEngine(placesRepository, fakeGeocodingRepository)

    class FakeGeocodingRepository : GeocodingRepository {
        var shouldFail = false
        var customCoordinates = mapOf(
            "buenos aires" to GeoPoint(-34.6037, -58.3816),
            "mendoza" to GeoPoint(-32.8894, -68.8458),
            "bariloche" to GeoPoint(-41.1335, -71.3103),
            "hotel céntrico, bariloche" to GeoPoint(-41.1340, -71.3110),
            "hotel, mendoza" to GeoPoint(-32.8900, -68.8465),
            "centro, mendoza" to GeoPoint(-32.8894, -68.8458)
        )

        override suspend fun geocode(query: String): Result<GeoPoint> {
            if (shouldFail) {
                return Result.failure(Exception("Red no disponible"))
            }
            val clean = query.trim().lowercase()
            val found = customCoordinates[clean]
            return if (found != null) {
                Result.success(found)
            } else {
                Result.failure(NoSuchElementException("No encontrado"))
            }
        }
    }

    @Test
    fun `test presupuesto gratuito yields free stops and free cost text`() = runTest {
        val prefs = TravelPreferences(
            destino = "Bariloche",
            intereses = setOf(TourismInterest.CULTURAL, TourismInterest.GASTRONOMICO),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "Hotel Céntrico",
            includeFoodStops = false,
            cantidadPersonas = 2,
            presupuesto = BudgetLevel.GRATUITO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)

        assertEquals("Gratuito o gasto opcional", result.costoTotalEstimado)
        val touristStops = result.actividades.filter { it.type == StopType.PLACE }
        assertTrue(touristStops.isNotEmpty())
        touristStops.forEach { stop ->
            assertEquals("Gratuito", stop.costoEstimado)
        }
    }

    @Test
    fun `test intereses multiples selects matching categories`() = runTest {
        val prefs = TravelPreferences(
            destino = "Bariloche",
            intereses = setOf(TourismInterest.NATURALEZA, TourismInterest.FOTOGRAFIA),
            movilidad = setOf(MobilityType.BICICLETA),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        val placeStops = result.actividades.filter { it.type == StopType.PLACE }

        val matches = placeStops.any { stop ->
            stop.descripcion.contains("Naturaleza") || stop.descripcion.contains("Fotografía") || stop.descripcion.contains("ecológica")
        }
        assertTrue(matches)
    }

    @Test
    fun `test rango horario corto genera menos paradas que rango largo`() = runTest {
        val basePrefs = TravelPreferences(
            destino = "Bariloche",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 660,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val shortResult = engine.generate(basePrefs)
        val longPrefs = basePrefs.copy(endMinutes = 1140)
        val longResult = engine.generate(longPrefs)

        val shortPlaceCount = shortResult.actividades.filter { it.type == StopType.PLACE }.size
        val longPlaceCount = longResult.actividades.filter { it.type == StopType.PLACE }.size

        assertTrue(longPlaceCount > shortPlaceCount)
    }

    @Test
    fun `test comida incluida inserta paradas de tipo FOOD`() = runTest {
        val prefs = TravelPreferences(
            destino = "Bariloche",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = true,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val resultWithFood = engine.generate(prefs)
        val foodStops = resultWithFood.actividades.filter { it.type == StopType.FOOD }
        assertTrue(foodStops.isNotEmpty())
    }

    @Test
    fun `test aventura y naturaleza prioriza actividades outdoor`() = runTest {
        val prefs = TravelPreferences(
            destino = "Mendoza",
            intereses = setOf(TourismInterest.AVENTURA),
            movilidad = setOf(MobilityType.MIXTO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        val placeStops = result.actividades.filter { it.type == StopType.PLACE }
        
        val hasOutdoor = placeStops.any { stop ->
            stop.visualType == ActivityVisualType.ADVENTURE || stop.visualType == ActivityVisualType.NATURE
        }
        assertTrue("Debería incluir al menos una actividad de aventura/outdoor", hasOutdoor)
    }

    @Test
    fun `test caminando mejora score de trekking y caminatas`() = runTest {
        val prefs = TravelPreferences(
            destino = "Mendoza",
            intereses = setOf(TourismInterest.AVENTURA),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        val placeStops = result.actividades.filter { it.type == StopType.PLACE }

        val containsTrekkingOrWalk = placeStops.any { stop ->
            stop.titulo.contains("Trekking") || stop.titulo.contains("Caminata")
        }
        assertTrue("El itinerario caminando debe preferir senderos o caminatas", containsTrekkingOrWalk)
    }

    @Test
    fun `test bicicleta mejora score de circuito recreativo`() = runTest {
        val prefs = TravelPreferences(
            destino = "Mendoza",
            intereses = setOf(TourismInterest.AVENTURA),
            movilidad = setOf(MobilityType.BICICLETA),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        val placeStops = result.actividades.filter { it.type == StopType.PLACE }

        val hasBikeCircuit = placeStops.any { stop ->
            stop.titulo.contains("Bicicleta")
        }
        assertTrue("El itinerario con bicicleta debe incluir el circuito de bicicleta recreativa", hasBikeCircuit)
    }

    @Test
    fun `test visualType asignado correctamente a ItineraryStop`() = runTest {
        val prefs = TravelPreferences(
            destino = "Mendoza",
            intereses = setOf(TourismInterest.AVENTURA, TourismInterest.GASTRONOMICO),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "Centro",
            includeFoodStops = true,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        
        val startStop = result.actividades.first { it.type == StopType.START }
        assertEquals(ActivityVisualType.START, startStop.visualType)

        val foodStops = result.actividades.filter { it.type == StopType.FOOD }
        foodStops.forEach { stop ->
            assertEquals(ActivityVisualType.FOOD, stop.visualType)
        }
    }

    @Test
    fun `test destino encontrado establece isFallbackCoordinates en false`() = runTest {
        val prefs = TravelPreferences(
            destino = "Buenos Aires",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        assertFalse(result.isFallbackCoordinates)
        // Coordenadas reales esperadas de Buenos Aires
        val startStop = result.actividades.first { it.type == StopType.START }
        assertEquals(-34.6037, startStop.latitud ?: 0.0, 0.0001)
    }

    @Test
    fun `test destino no encontrado usa fallback local`() = runTest {
        val prefs = TravelPreferences(
            destino = "Ciudad Inexistente",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        assertTrue(result.isFallbackCoordinates)
        // Las actividades deberían posicionarse igual usando el fallback por hash
        val startStop = result.actividades.first { it.type == StopType.START }
        assertTrue(startStop.latitud != 0.0 && startStop.longitud != 0.0)
    }

    @Test
    fun `test punto de partida geocodificado`() = runTest {
        val prefs = TravelPreferences(
            destino = "Mendoza",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "Hotel",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        val startStop = result.actividades.first { it.type == StopType.START }
        // Se espera la latitud mock del hotel en mendoza (-32.8900)
        assertEquals(-32.8900, startStop.latitud ?: 0.0, 0.0001)
    }

    @Test
    fun `test punto de partida vacio usa centro de destino`() = runTest {
        val prefs = TravelPreferences(
            destino = "Mendoza",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 1080,
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)
        val startStop = result.actividades.first { it.type == StopType.START }
        // Se espera el centro de Mendoza (-32.8894)
        assertEquals(-32.8894, startStop.latitud ?: 0.0, 0.0001)
    }
}
