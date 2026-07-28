package com.wayfii.app.data.repository

import com.wayfii.app.data.model.ActivityVisualType
import com.wayfii.app.data.model.BudgetLevel
import com.wayfii.app.data.model.Itinerary
import com.wayfii.app.data.model.ItineraryStop
import com.wayfii.app.data.model.MobilityType
import com.wayfii.app.data.model.StopType
import com.wayfii.app.data.model.TourismInterest
import com.wayfii.app.data.model.TravelPace
import com.wayfii.app.data.model.TravelPreferences
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RemoteItineraryRepository(
    baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
) : ItineraryRepository {

    private val endpoint = baseUrl.trim().trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun generarItinerario(preferences: TravelPreferences): Result<Itinerary> =
        withContext(Dispatchers.IO) {
            try {
                require(endpoint.isNotEmpty()) { "La API de Wayfii no está configurada." }
                val payload = PlanItineraryRequestDto.from(preferences)
                val request = Request.Builder()
                    .url("$endpoint/v1/itineraries/plan")
                    .header("Accept", "application/json")
                    .header("User-Agent", "WayfiiAndroid/1.0")
                    .post(
                        json.encodeToString(payload)
                            .toRequestBody(JSON_MEDIA_TYPE),
                    )
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val apiMessage = runCatching {
                            json.decodeFromString<ApiErrorDto>(body).error
                        }.getOrNull()
                        throw IOException(
                            apiMessage ?: "La API respondió con código ${response.code}.",
                        )
                    }
                    Result.success(
                        json.decodeFromString<PlannedItineraryDto>(body).toModel(preferences),
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            }
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}

@Serializable
private data class PlanItineraryRequestDto(
    val destination: String,
    val center: GeoPointDto? = null,
    val originName: String? = null,
    val interests: List<String>,
    val mobility: List<String>,
    val startMinutes: Int,
    val endMinutes: Int,
    val people: Int,
    val budget: String,
    val pace: String,
    val includeFood: Boolean,
) {
    companion object {
        fun from(preferences: TravelPreferences): PlanItineraryRequestDto {
            val latitude = preferences.lat
            val longitude = preferences.lon
            val center = if (latitude != null && longitude != null) {
                GeoPointDto(latitude, longitude)
            } else {
                null
            }
            return PlanItineraryRequestDto(
                destination = preferences.destino.trim(),
                center = center,
                originName = preferences.startingPointName.trim().ifEmpty { null },
                interests = preferences.intereses.map(TourismInterest::apiValue),
                mobility = preferences.movilidad.map(MobilityType::apiValue),
                startMinutes = preferences.startMinutes,
                endMinutes = preferences.endMinutes,
                people = preferences.cantidadPersonas,
                budget = preferences.presupuesto.apiValue(),
                pace = preferences.ritmo.apiValue(),
                includeFood = preferences.includeFoodStops,
            )
        }
    }
}

@Serializable
private data class GeoPointDto(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
private data class MoneyDto(
    val amount: Double,
    val currency: String,
)

@Serializable
private data class ItineraryStopDto(
    val order: Int,
    val placeId: String,
    val type: String,
    val title: String,
    val summary: String,
    val location: GeoPointDto,
    val startsAtMinutes: Int,
    val durationMinutes: Int,
    val estimatedCost: MoneyDto? = null,
    val category: String,
    val imageUrl: String? = null,
    val reason: String,
)

@Serializable
private data class PlannedItineraryDto(
    val destination: String,
    val center: GeoPointDto,
    val origin: GeoPointDto,
    val stops: List<ItineraryStopDto>,
    val startMinutes: Int,
    val endMinutes: Int,
    val estimatedCost: MoneyDto? = null,
    val dataSourceSummary: String,
) {
    fun toModel(preferences: TravelPreferences): Itinerary {
        val startName = preferences.startingPointName.trim()
            .ifEmpty { "Centro de la ciudad" }
        val activities = buildList {
            add(
                ItineraryStop(
                    order = 1,
                    type = StopType.START,
                    visualType = ActivityVisualType.START,
                    horaInicio = formatClock(startMinutes),
                    titulo = "Inicio: $startName",
                    descripcion = "Comienzo del recorrido de exploración.",
                    duracionEstimada = "0m",
                    costoEstimado = "Gratuito",
                    latitud = origin.latitude,
                    longitud = origin.longitude,
                    reason = "Punto de partida resuelto por Wayfii.",
                ),
            )
            stops.forEach { stop ->
                val stopType = if (stop.type == "food") StopType.FOOD else StopType.PLACE
                add(
                    ItineraryStop(
                        order = stop.order + 1,
                        type = stopType,
                        visualType = stop.visualType(),
                        horaInicio = formatClock(stop.startsAtMinutes),
                        titulo = stop.title,
                        descripcion = stop.summary,
                        duracionEstimada = formatDuration(stop.durationMinutes),
                        costoEstimado = stop.estimatedCost.formatCost(),
                        latitud = stop.location.latitude,
                        longitud = stop.location.longitude,
                        reason = stop.reason,
                        imageUrl = stop.imageUrl?.takeIf(String::isNotBlank),
                        placeId = stop.placeId.takeIf(String::isNotBlank),
                    ),
                )
            }
        }

        return Itinerary(
            destino = destination,
            actividades = activities,
            duracionTotal = formatAvailableDuration(endMinutes - startMinutes),
            costoTotalEstimado = estimatedCost.formatTotalCost(),
            puntoPartida = startName,
            rangoHorarioText = "${formatClock(startMinutes)} a ${formatClock(endMinutes)}",
            incluyeComida = preferences.includeFoodStops,
            cantidadPersonas = preferences.cantidadPersonas,
            isFallbackCoordinates = false,
            dataSourceSummary = dataSourceSummary,
        )
    }
}

@Serializable
private data class ApiErrorDto(
    val error: String,
)

private fun ItineraryStopDto.visualType(): ActivityVisualType {
    if (type == "food") return ActivityVisualType.FOOD
    return when (category) {
        "culture" -> ActivityVisualType.CULTURE
        "history" -> ActivityVisualType.HISTORY
        "gastronomy" -> ActivityVisualType.FOOD
        "nature" -> ActivityVisualType.NATURE
        "adventure" -> ActivityVisualType.ADVENTURE
        "shopping" -> ActivityVisualType.SHOPPING
        "photography" -> ActivityVisualType.PHOTO
        "events" -> ActivityVisualType.EVENT
        "family" -> ActivityVisualType.FAMILY
        "popular" -> ActivityVisualType.MAINSTREAM
        else -> ActivityVisualType.DEFAULT
    }
}

private fun TourismInterest.apiValue(): String = when (this) {
    TourismInterest.CLASICO -> "classic"
    TourismInterest.ALTERNATIVO -> "alternative"
    TourismInterest.MAINSTREAM -> "popular"
    TourismInterest.CULTURAL -> "culture"
    TourismInterest.GASTRONOMICO -> "gastronomy"
    TourismInterest.NATURALEZA -> "nature"
    TourismInterest.FAMILIAR -> "family"
    TourismInterest.HISTORIA -> "history"
    TourismInterest.COMPRAS -> "shopping"
    TourismInterest.FOTOGRAFIA -> "photography"
    TourismInterest.EVENTOS -> "events"
    TourismInterest.AVENTURA -> "adventure"
}

private fun MobilityType.apiValue(): String = when (this) {
    MobilityType.CAMINANDO -> "walking"
    MobilityType.TRANSPORTE_PUBLICO -> "public_transport"
    MobilityType.AUTO -> "car"
    MobilityType.BICICLETA -> "bicycle"
    MobilityType.TAXI_APP -> "taxi_app"
    MobilityType.MIXTO -> "mixed"
}

private fun BudgetLevel.apiValue(): String = when (this) {
    BudgetLevel.GRATUITO -> "free"
    BudgetLevel.BAJO -> "low"
    BudgetLevel.MEDIO -> "medium"
    BudgetLevel.ALTO -> "high"
}

private fun TravelPace.apiValue(): String = when (this) {
    TravelPace.TRANQUILO -> "relaxed"
    TravelPace.EQUILIBRADO -> "balanced"
    TravelPace.INTENSO -> "intense"
}

private fun formatClock(minutes: Int): String = String.format(
    Locale.getDefault(),
    "%02d:%02d",
    minutes / 60,
    minutes % 60,
)

private fun formatDuration(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours == 0 -> "${remaining}m"
        remaining == 0 -> "${hours}h"
        else -> "${hours}h ${remaining}m"
    }
}

private fun formatAvailableDuration(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return if (remaining == 0) {
        "$hours horas disponibles"
    } else {
        "$hours h $remaining min disponibles"
    }
}

private fun MoneyDto?.formatCost(): String = when {
    this == null || amount == 0.0 -> "Gratuito"
    else -> "${String.format(Locale.getDefault(), "%.0f", amount)} $currency"
}

private fun MoneyDto?.formatTotalCost(): String = when {
    this == null || amount == 0.0 -> "Gratuito o gasto opcional"
    else -> "Est. ${String.format(Locale.getDefault(), "%.0f", amount)} $currency"
}
