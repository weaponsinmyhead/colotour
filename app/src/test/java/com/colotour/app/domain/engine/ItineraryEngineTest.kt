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
            destino = "Buenos Aires",
            intereses = setOf(TourismInterest.CULTURAL, TourismInterest.GASTRONOMICO),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540, // 09:00
            endMinutes = 1080,  // 18:00
            startingPointName = "Hotel Obelisco",
            includeFoodStops = false,
            cantidadPersonas = 2,
            presupuesto = BudgetLevel.GRATUITO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val result = engine.generate(prefs)

        assertEquals("Gratuito o gasto opcional", result.costoTotalEstimado)
        // Exceptuando el inicio, todas las paradas turísticas deben ser gratuitas
        val touristStops = result.actividades.filter { it.type == StopType.PLACE }
        assertTrue(touristStops.isNotEmpty())
        touristStops.forEach { stop ->
            assertEquals("Gratuito", stop.costoEstimado)
        }
    }

    @Test
    fun `test intereses multiples selects matching categories`() = runTest {
        val prefs = TravelPreferences(
            destino = "Buenos Aires",
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

        // Debería priorizar paradas que sean de naturaleza o fotografía
        val matches = placeStops.any { stop ->
            stop.descripcion.contains("Naturaleza") || stop.descripcion.contains("Fotografía")
        }
        assertTrue(matches)
    }

    @Test
    fun `test rango horario corto genera menos paradas que rango largo`() = runTest {
        val basePrefs = TravelPreferences(
            destino = "Buenos Aires",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,
            endMinutes = 660, // Rango de 2 horas (corto)
            startingPointName = "",
            includeFoodStops = false,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val shortResult = engine.generate(basePrefs)

        val longPrefs = basePrefs.copy(endMinutes = 1140) // Rango de 10 horas (largo)
        val longResult = engine.generate(longPrefs)

        // El rango largo debe tener más paradas turísticas que el corto
        val shortPlaceCount = shortResult.actividades.filter { it.type == StopType.PLACE }.size
        val longPlaceCount = longResult.actividades.filter { it.type == StopType.PLACE }.size

        assertTrue(longPlaceCount > shortPlaceCount)
    }

    @Test
    fun `test comida incluida inserta paradas de tipo FOOD`() = runTest {
        val prefs = TravelPreferences(
            destino = "Buenos Aires",
            intereses = setOf(TourismInterest.CULTURAL),
            movilidad = setOf(MobilityType.CAMINANDO),
            startMinutes = 540,  // 09:00
            endMinutes = 1080,  // 18:00 (Cruza la hora de almuerzo)
            startingPointName = "",
            includeFoodStops = true,
            cantidadPersonas = 1,
            presupuesto = BudgetLevel.MEDIO,
            ritmo = TravelPace.EQUILIBRADO
        )

        val resultWithFood = engine.generate(prefs)
        val foodStops = resultWithFood.actividades.filter { it.type == StopType.FOOD }
        assertTrue(foodStops.isNotEmpty())

        val resultNoFood = engine.generate(prefs.copy(includeFoodStops = false))
        val noFoodStops = resultNoFood.actividades.filter { it.type == StopType.FOOD }
        assertTrue(noFoodStops.isEmpty())
    }
}
