package com.colotour.app.domain.engine

import kotlin.math.pow
import kotlin.math.sqrt

class RouteOptimizer {
    fun optimizeRoute(places: List<CandidatePlace>): List<CandidatePlace> {
        if (places.isEmpty()) return emptyList()

        val unvisited = places.toMutableList()
        val optimized = mutableListOf<CandidatePlace>()

        // Empezamos en el lugar de mayor puntuación (primer elemento)
        var current = unvisited.removeAt(0)
        optimized.add(current)

        while (unvisited.isNotEmpty()) {
            val nextIndex = findNearestIndex(current, unvisited)
            current = unvisited.removeAt(nextIndex)
            optimized.add(current)
        }

        return optimized
    }

    private fun findNearestIndex(origin: CandidatePlace, candidates: List<CandidatePlace>): Int {
        var minDistance = Double.MAX_VALUE
        var minIndex = 0

        for (i in candidates.indices) {
            val dist = calculateDistance(origin, candidates[i])
            if (dist < minDistance) {
                minDistance = dist
                minIndex = i
            }
        }
        return minIndex
    }

    private fun calculateDistance(p1: CandidatePlace, p2: CandidatePlace): Double {
        // Distancia euclidiana aproximada para tramos urbanos
        return sqrt((p1.latitud - p2.latitud).pow(2) + (p1.longitud - p2.longitud).pow(2))
    }
}
