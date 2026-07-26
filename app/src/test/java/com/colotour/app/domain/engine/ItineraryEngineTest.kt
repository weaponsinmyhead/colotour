package com.colotour.app.domain.engine

import com.colotour.app.data.model.*
import com.colotour.app.data.repository.MockPlacesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ItineraryEngineTest {

    private val placesRepository = MockPlacesRepository()
    private val engine = ItineraryEngine(placesRepository)

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

        // Debería priorizar actividades peatonales/senderos
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
        
        // El punto de partida debe tener START
        val startStop = result.actividades.first { it.type == StopType.START }
        assertEquals(ActivityVisualType.START, startStop.visualType)

        // Las comidas deben tener FOOD
        val foodStops = result.actividades.filter { it.type == StopType.FOOD }
        foodStops.forEach { stop ->
            assertEquals(ActivityVisualType.FOOD, stop.visualType)
        }

        // Las paradas de aventura deben tener ADVENTURE
        val adventureStops = result.actividades.filter { it.titulo.contains("Sendero") || it.titulo.contains("Mirador del Cerro") }
        adventureStops.forEach { stop ->
            assertEquals(ActivityVisualType.ADVENTURE, stop.visualType)
        }
    }
}
