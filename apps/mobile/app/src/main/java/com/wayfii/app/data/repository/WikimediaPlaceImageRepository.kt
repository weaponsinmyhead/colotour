package com.wayfii.app.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

open class WikimediaPlaceImageRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) : PlaceImageRepository {

    private val cache = ConcurrentHashMap<String, String?>()
    private val jsonParser = Json { ignoreUnknownKeys = true }

    override suspend fun findImageForPlace(placeName: String, destination: String): Result<String?> = withContext(Dispatchers.IO) {
        val cleanQuery = "$placeName $destination".trim().lowercase()
        if (cleanQuery.isEmpty()) {
            return@withContext Result.success(null)
        }

        // Retornar si ya existe en cache (incluso si se resolvió como null)
        if (cache.containsKey(cleanQuery)) {
            return@withContext Result.success(cache[cleanQuery])
        }

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val url = "https://es.wikipedia.org/w/api.php?action=query&format=json&prop=pageimages&piprop=thumbnail&pithumbsize=400&generator=search&gsrsearch=$encodedQuery&gsrlimit=1"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", WAYFII_OPEN_DATA_USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error code: ${response.code}"))
                }

                val bodyString = response.body?.string() ?: ""
                val json = jsonParser.parseToJsonElement(bodyString).jsonObject
                val queryObj = json["query"]?.jsonObject
                val pagesObj = queryObj?.get("pages")?.jsonObject
                
                var imageUrl: String? = null
                if (pagesObj != null && pagesObj.isNotEmpty()) {
                    val firstPageKey = pagesObj.keys.first()
                    val firstPage = pagesObj[firstPageKey]?.jsonObject
                    val thumbnail = firstPage?.get("thumbnail")?.jsonObject
                    imageUrl = thumbnail?.get("source")?.jsonPrimitive?.content
                }

                cache[cleanQuery] = imageUrl
                Result.success(imageUrl)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
