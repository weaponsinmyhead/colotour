package com.wayfii.app.domain.engine.discovery

import com.wayfii.app.data.model.*

class AdventureScoreEvaluator {

    fun evaluateScore(
        proposal: AdventureProposal,
        context: ContextEnvironment,
        theme: AdventureTheme?,
        activeMoment: CityMoment?
    ): AdventureScore {
        var seasonScore = 12.0
        var weatherScore = 12.0
        var goldenHourScore = 12.0
        var walkingComfortScore = 13.0
        var storyOriginalityScore = 8.0
        var poiDiversityScore = 8.0
        var photoPotentialScore = 8.0
        var contextUniquenessScore = 8.0

        // 1. Season relevance boost
        if (activeMoment?.season == context.season || proposal.title.contains(context.season.displayName, ignoreCase = true)) {
            seasonScore = 15.0
        }

        // 2. Weather compatibility boost
        if (context.weather == WeatherCondition.RAIN) {
            val isRainProof = proposal.highlights.any { it.contains("techad", ignoreCase = true) || it.contains("cubiert", ignoreCase = true) } ||
                    proposal.introNarrative.contains("resguard", ignoreCase = true)
            weatherScore = if (isRainProof) 15.0 else 7.0
        } else if (context.weather == WeatherCondition.SUNNY) {
            weatherScore = 15.0
        }

        // 3. Golden Hour alignment
        if (context.timeOfDay == TimeOfDay.GOLDEN_HOUR) {
            if (theme?.id == "golden_hour" || proposal.title.contains("atardecer", ignoreCase = true) || proposal.title.contains("dorad", ignoreCase = true)) {
                goldenHourScore = 15.0
            }
        }

        // 4. City Moment active boost
        if (activeMoment != null) {
            contextUniquenessScore = 10.0
            storyOriginalityScore = 10.0
        }

        // 5. POI Diversity
        val stopTypes = proposal.mainQuestStops.map { it.visualType }.toSet()
        poiDiversityScore = (stopTypes.size * 2.5).coerceAtMost(10.0)

        val total = (seasonScore + weatherScore + goldenHourScore + walkingComfortScore +
                storyOriginalityScore + poiDiversityScore + photoPotentialScore + contextUniquenessScore).coerceIn(50.0, 99.0)

        val label = when {
            total >= 92.0 -> "⚡ ${total.toInt()} pts · Contexto Ideal"
            total >= 85.0 -> "⭐ ${total.toInt()} pts · Muy Recomendado"
            else -> "🌿 ${total.toInt()} pts · Paseo Relajado"
        }

        return AdventureScore(
            totalScore = total,
            seasonRelevance = seasonScore,
            weatherCompatibility = weatherScore,
            goldenHourAlignment = goldenHourScore,
            walkingComfort = walkingComfortScore,
            storyOriginality = storyOriginalityScore,
            poiDiversity = poiDiversityScore,
            photographyPotential = photoPotentialScore,
            contextUniqueness = contextUniquenessScore,
            scoreLabel = label
        )
    }
}
