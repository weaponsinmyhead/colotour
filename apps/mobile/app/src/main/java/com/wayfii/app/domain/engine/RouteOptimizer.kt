package com.wayfii.app.domain.engine

import kotlin.math.pow
import kotlin.math.sqrt

class RouteOptimizer {
    fun optimizeRoute(startPoint: StartPoint, places: List<CandidatePlace>): List<CandidatePlace> {
        if (places.isEmpty()) return emptyList()

        val unvisited = places.toMutableList()
        val optimized = mutableListOf<CandidatePlace>()

        // Empezamos buscando desde la ubicación del punto de partida
        var currentLat = startPoint.latitude
        var currentLon = startPoint.longitude

        while (unvisited.isNotEmpty()) {
            var minDistance = Double.MAX_VALUE
            var minIndex = 0

            for (i in unvisited.indices) {
                val dist = sqrt((currentLat - unvisited[i].latitud).pow(2) + (currentLon - unvisited[i].longitud).pow(2))
                if (dist < minDistance) {
                    minDistance = dist
                    minIndex = i
                }
            }

            val nextPlace = unvisited.removeAt(minIndex)
            optimized.add(nextPlace)
            currentLat = nextPlace.latitud
            currentLon = nextPlace.longitud
        }

        return optimized
    }
}
