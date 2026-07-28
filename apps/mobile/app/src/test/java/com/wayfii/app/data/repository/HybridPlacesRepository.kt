package com.wayfii.app.data.repository

import com.wayfii.app.domain.engine.CandidatePlace

// Composición histórica conservada solo para pruebas de regresión.
class HybridPlacesRepository(
    private val overpassRepository: OverpassPlacesRepository,
    private val mockRepository: MockPlacesRepository
) : PlacesRepository {

    // Almacena un resumen de la última procedencia de los datos para la UI
    var lastSourceSummary = ""
        private set

    override suspend fun getCandidatePlaces(destino: String, baseLat: Double, baseLon: Double): List<CandidatePlace> {
        val overpassResult = overpassRepository.getCandidatePlaces(destino, baseLat, baseLon)
        
        if (overpassResult.isSuccess) {
            val realPlaces = overpassResult.getOrThrow()
            if (realPlaces.size >= 5) {
                lastSourceSummary = "Incluye lugares reales de OpenStreetMap"
                return realPlaces
            } else if (realPlaces.isNotEmpty()) {
                // Combinar reales y mocks si los resultados reales son insuficientes (< 5)
                lastSourceSummary = "Lugares reales combinados con sugerencias aproximadas"
                val mocks = mockRepository.getCandidatePlaces(destino, baseLat, baseLon)
                
                val combined = realPlaces.toMutableList()
                val realNames = realPlaces.map { it.nombre.lowercase().trim() }.toSet()
                
                mocks.forEach { mock ->
                    if (!realNames.contains(mock.nombre.lowercase().trim())) {
                        combined.add(mock)
                    }
                }
                return combined
            }
        }
        
        // Fallback completo a mocks ante errores o sin resultados reales
        lastSourceSummary = "Algunos lugares son sugerencias aproximadas"
        return mockRepository.getCandidatePlaces(destino, baseLat, baseLon)
    }
}
