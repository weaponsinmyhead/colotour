package com.colotour.app.domain.engine

import com.colotour.app.data.model.BudgetLevel
import com.colotour.app.data.model.TourismInterest
import com.colotour.app.data.repository.HybridPlacesRepository
import com.colotour.app.data.repository.MockPlacesRepository
import com.colotour.app.data.repository.OverpassPlacesRepository
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HybridPlacesRepositoryTest {

    private val mockRepo = MockPlacesRepository()

    private class FakeOverpassPlacesRepository(
        private val resultToReturn: Result<List<CandidatePlace>>
    ) : OverpassPlacesRepository(OkHttpClient()) {
        override suspend fun getCandidatePlaces(
            destino: String,
            baseLat: Double,
            baseLon: Double
        ): Result<List<CandidatePlace>> {
            return resultToReturn
        }
    }

    @Test
    fun `test overpass devuelve suficientes resultados y usa reales`() = runTest {
        val realList = List(6) { index ->
            CandidatePlace(
                id = "ov-$index",
                nombre = "Real Place $index",
                descripcion = "Real desc",
                latitud = 10.0,
                longitud = 10.0,
                estilo = TourismInterest.CULTURAL,
                presupuesto = BudgetLevel.BAJO,
                duracionRecomendadaMinutos = 60,
                costoBasePorPersona = 5.0,
                popularidad = 0.8
            )
        }
        val overpassFake = FakeOverpassPlacesRepository(Result.success(realList))
        val hybridRepo = HybridPlacesRepository(overpassFake, mockRepo)

        val result = hybridRepo.getCandidatePlaces("Mendoza", -32.8894, -68.8458)

        assertEquals(6, result.size)
        assertTrue(result.all { it.id.startsWith("ov-") })
        assertEquals("Incluye lugares reales de OpenStreetMap", hybridRepo.lastSourceSummary)
    }

    @Test
    fun `test overpass devuelve pocos resultados y combina reales con mocks`() = runTest {
        val realList = listOf(
            CandidatePlace(
                id = "ov-1",
                nombre = "Real Place 1",
                descripcion = "Real desc",
                latitud = 10.0,
                longitud = 10.0,
                estilo = TourismInterest.CULTURAL,
                presupuesto = BudgetLevel.BAJO,
                duracionRecomendadaMinutos = 60,
                costoBasePorPersona = 5.0,
                popularidad = 0.8
            )
        )
        val overpassFake = FakeOverpassPlacesRepository(Result.success(realList))
        val hybridRepo = HybridPlacesRepository(overpassFake, mockRepo)

        val result = hybridRepo.getCandidatePlaces("Mendoza", -32.8894, -68.8458)

        assertTrue(result.size > 1)
        assertTrue(result.any { it.id.startsWith("ov-") })
        assertTrue(result.any { !it.id.startsWith("ov-") })
        assertEquals("Lugares reales combinados con sugerencias aproximadas", hybridRepo.lastSourceSummary)
    }

    @Test
    fun `test overpass falla y usa mock`() = runTest {
        val overpassFake = FakeOverpassPlacesRepository(Result.failure(Exception("Red caida")))
        val hybridRepo = HybridPlacesRepository(overpassFake, mockRepo)

        val result = hybridRepo.getCandidatePlaces("Mendoza", -32.8894, -68.8458)

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { !it.id.startsWith("ov-") })
        assertEquals("Algunos lugares son sugerencias aproximadas", hybridRepo.lastSourceSummary)
    }
}
