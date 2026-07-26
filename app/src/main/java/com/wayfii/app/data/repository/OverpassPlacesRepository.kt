package com.wayfii.app.data.repository

import com.wayfii.app.data.model.BudgetLevel
import com.wayfii.app.data.model.TourismInterest
import com.wayfii.app.domain.engine.CandidatePlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

open class OverpassPlacesRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
) {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, List<CandidatePlace>>()

    open suspend fun getCandidatePlaces(destino: String, baseLat: Double, baseLon: Double): Result<List<CandidatePlace>> = withContext(Dispatchers.IO) {
        val roundedLat = (baseLat * 100.0).roundToInt() / 100.0
        val roundedLon = (baseLon * 100.0).roundToInt() / 100.0
        val cacheKey = "$destino-$roundedLat-$roundedLon"

        // Caché en memoria por destino y coordenadas truncadas
        cache[cacheKey]?.let {
            return@withContext Result.success(it)
        }

        // Consulta de nodos de interés turísticos en un radio de 3 km
        val query = """
            [out:json][timeout:8];
            (
              node["tourism"~"museum|attraction|viewpoint|gallery|artwork"](around:3000,$baseLat,$baseLon);
              node["historic"](around:3000,$baseLat,$baseLon);
              node["leisure"~"park|nature_reserve"](around:3000,$baseLat,$baseLon);
              node["natural"~"beach"](around:3000,$baseLat,$baseLon);
              node["amenity"~"restaurant|cafe|theatre|marketplace"](around:3000,$baseLat,$baseLon);
              node["shop"](around:3000,$baseLat,$baseLon);
            );
            out body;
        """.trimIndent()

        try {
            val body = FormBody.Builder()
                .add("data", query)
                .build()

            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(body)
                .header("User-Agent", "WayfiiApp/1.0 (milla.developer@example.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Error de red Overpass: ${response.code}"))
                }

                val bodyString = response.body?.string() ?: ""
                val overpassRes = jsonParser.decodeFromString<OverpassResponse>(bodyString)
                
                val places = overpassRes.elements
                    .filter { !it.tags["name"].isNullOrBlank() } // Se descartan nodos sin nombre
                    .map { elem ->
                        mapElementToCandidate(elem)
                    }

                cache[cacheKey] = places
                Result.success(places)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapElementToCandidate(elem: OverpassElement): CandidatePlace {
        val tags = elem.tags
        val name = tags["name"] ?: "Lugar de interés"
        
        val estilo = when {
            tags["tourism"] == "museum" -> TourismInterest.CULTURAL
            tags["tourism"] == "viewpoint" -> TourismInterest.FOTOGRAFIA
            tags["tourism"] in listOf("gallery", "artwork") -> TourismInterest.CULTURAL
            tags.containsKey("historic") -> TourismInterest.HISTORIA
            tags["leisure"] == "park" -> TourismInterest.NATURALEZA
            tags["leisure"] == "nature_reserve" -> TourismInterest.AVENTURA
            tags["natural"] == "beach" -> TourismInterest.NATURALEZA
            tags["amenity"] in listOf("restaurant", "cafe") -> TourismInterest.GASTRONOMICO
            tags["amenity"] == "theatre" -> TourismInterest.CULTURAL
            tags["amenity"] == "marketplace" -> TourismInterest.GASTRONOMICO
            tags.containsKey("shop") -> TourismInterest.COMPRAS
            else -> TourismInterest.CLASICO
        }

        val isFree = tags["fee"] == "no" || tags["admission"] == "free" || 
                estilo in listOf(TourismInterest.NATURALEZA, TourismInterest.FOTOGRAFIA, TourismInterest.AVENTURA)
        
        val presupuesto = when {
            isFree -> BudgetLevel.GRATUITO
            estilo == TourismInterest.GASTRONOMICO -> BudgetLevel.MEDIO
            estilo == TourismInterest.COMPRAS -> BudgetLevel.MEDIO
            else -> BudgetLevel.BAJO
        }

        val costoBase = when (presupuesto) {
            BudgetLevel.GRATUITO -> 0.0
            BudgetLevel.BAJO -> 5.0
            BudgetLevel.MEDIO -> 15.0
            BudgetLevel.ALTO -> 35.0
        }

        val duracion = when {
            tags["tourism"] == "museum" -> 120
            tags["tourism"] == "viewpoint" -> 30
            estilo == TourismInterest.NATURALEZA -> 90
            estilo == TourismInterest.GASTRONOMICO -> 60
            estilo == TourismInterest.COMPRAS -> 45
            else -> 60
        }

        val desc = tags["description"] ?: when (estilo) {
            TourismInterest.CULTURAL -> "Espacio cultural de la ciudad."
            TourismInterest.NATURALEZA -> "Entorno natural y áreas recreativas."
            TourismInterest.GASTRONOMICO -> "Sabores y platos regionales."
            TourismInterest.FOTOGRAFIA -> "Lugar fotográfico con excelentes visuales."
            TourismInterest.AVENTURA -> "Senderismo y recreación al aire libre."
            else -> "Punto turístico recomendado para visitar."
        }

        return CandidatePlace(
            id = "ov-${elem.id}",
            nombre = name,
            descripcion = desc,
            latitud = elem.lat,
            longitud = elem.lon,
            estilo = estilo,
            presupuesto = presupuesto,
            duracionRecomendadaMinutos = duracion,
            costoBasePorPersona = costoBase,
            popularidad = 0.8
        )
    }

    @Serializable
    private data class OverpassResponse(
        val elements: List<OverpassElement> = emptyList()
    )

    @Serializable
    private data class OverpassElement(
        val id: Long,
        val lat: Double,
        val lon: Double,
        val tags: Map<String, String> = emptyMap()
    )
}
