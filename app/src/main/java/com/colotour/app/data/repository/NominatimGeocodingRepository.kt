package com.colotour.app.data.repository

import com.colotour.app.data.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class NominatimGeocodingRepository : GeocodingRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, GeoPoint>()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    override suspend fun geocode(query: String): Result<GeoPoint> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Query vacía."))
        }

        // Buscar en cache local
        cache[cleanQuery]?.let {
            return@withContext Result.success(it)
        }

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=1"

            val request = Request.Builder()
                .url(url)
                // Se incluye un agente descriptivo para evitar bloqueos del servidor OSM
                .header("User-Agent", "ColotourTravelApp/1.0 (milla.developer@example.com)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Error HTTP: ${response.code}"))
                }

                val bodyString = response.body?.string() ?: ""
                val jsonArray = jsonParser.parseToJsonElement(bodyString).jsonArray

                if (jsonArray.isEmpty()) {
                    return@withContext Result.failure(NoSuchElementException("Sin resultados para la consulta."))
                }

                val firstResult = jsonArray[0].jsonObject
                val lat = firstResult["lat"]?.jsonPrimitive?.content?.toDoubleOrNull()
                val lon = firstResult["lon"]?.jsonPrimitive?.content?.toDoubleOrNull()

                if (lat != null && lon != null) {
                    val geoPoint = GeoPoint(lat, lon)
                    cache[cleanQuery] = geoPoint
                    Result.success(geoPoint)
                } else {
                    Result.failure(IllegalStateException("Coordenadas nulas o formato erróneo."))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
