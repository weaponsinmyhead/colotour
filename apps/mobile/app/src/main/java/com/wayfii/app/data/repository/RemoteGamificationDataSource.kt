package com.wayfii.app.data.repository

import com.wayfii.app.data.model.GamificationActivity
import com.wayfii.app.data.model.GamificationSyncStatus
import com.wayfii.app.data.model.PlayerProgress
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

internal data class RemoteActivityReward(
    val recorded: Boolean,
    val awardedPoints: Int,
    val earnedBadges: List<String>,
    val profile: PlayerProgress,
)

internal interface GamificationRemoteDataSource {
    suspend fun recordActivity(
        playerId: String,
        activity: GamificationActivity,
    ): Result<RemoteActivityReward>

    suspend fun getPlayer(playerId: String): Result<PlayerProgress>
}

internal class RemoteGamificationDataSource(
    baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
) : GamificationRemoteDataSource {
    private val endpoint = baseUrl.trim().trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun recordActivity(
        playerId: String,
        activity: GamificationActivity,
    ): Result<RemoteActivityReward> = execute {
        val payload = ActivityRequestDto(
            idempotencyKey = activity.idempotencyKey,
            userId = playerId,
            type = activity.type.name.lowercase(Locale.US),
            subjectId = activity.subjectId,
            metadata = activity.metadata,
            occurredAt = formatRfc3339(activity.occurredAtEpochMillis),
        )
        val request = Request.Builder()
            .url("$endpoint/v1/gamification/activities")
            .header("Accept", "application/json")
            .header("User-Agent", "WayfiiAndroid/1.0")
            .post(
                json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE),
            )
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, response.isSuccessful, body)
            json.decodeFromString<RecordActivityResponseDto>(body).toModel()
        }
    }

    override suspend fun getPlayer(playerId: String): Result<PlayerProgress> = execute {
        val encodedPlayer = URLEncoder.encode(playerId, Charsets.UTF_8.name())
        val request = Request.Builder()
            .url("$endpoint/v1/gamification/players/$encodedPlayer")
            .header("Accept", "application/json")
            .header("User-Agent", "WayfiiAndroid/1.0")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            ensureSuccessful(response.code, response.isSuccessful, body)
            json.decodeFromString<PlayerProfileDto>(body).toModel()
        }
    }

    private suspend fun <T> execute(block: () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            require(endpoint.isNotEmpty()) { "La API de Wayfii no está configurada." }
            Result.success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun ensureSuccessful(
        responseCode: Int,
        isSuccessful: Boolean,
        body: String,
    ) {
        if (isSuccessful) return
        val apiMessage = runCatching {
            json.decodeFromString<GamificationApiErrorDto>(body).error
        }.getOrNull()
        throw IOException(apiMessage ?: "La API respondió con código $responseCode.")
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}

@Serializable
private data class ActivityRequestDto(
    val idempotencyKey: String,
    val userId: String,
    val type: String,
    val subjectId: String,
    val metadata: Map<String, String>,
    val occurredAt: String,
)

@Serializable
private data class RecordActivityResponseDto(
    val recorded: Boolean,
    val awardedPoints: Int = 0,
    val earnedBadges: List<String> = emptyList(),
    val profile: PlayerProfileDto,
) {
    fun toModel(): RemoteActivityReward = RemoteActivityReward(
        recorded = recorded,
        awardedPoints = awardedPoints,
        earnedBadges = earnedBadges,
        profile = profile.toModel(),
    )
}

@Serializable
private data class PlayerProfileDto(
    val userId: String,
    val points: Int,
    val level: Int,
    val currentStreak: Int,
    val badges: List<String> = emptyList(),
    val lastActivityAt: String? = null,
) {
    fun toModel(): PlayerProgress = PlayerProgress(
        points = points,
        level = level,
        currentStreak = currentStreak,
        badges = badges,
        lastActivityAtEpochMillis = parseRfc3339(lastActivityAt),
        pendingSyncCount = 0,
        syncStatus = GamificationSyncStatus.SYNCED,
    )
}

@Serializable
private data class GamificationApiErrorDto(
    val error: String,
)

private fun formatRfc3339(epochMillis: Long): String =
    rfc3339Formatter().format(Date(epochMillis))

private fun parseRfc3339(value: String?): Long {
    if (value.isNullOrBlank() || value.startsWith("0001-")) return 0
    val normalized = value.replace(
        Regex("""\.(\d{3})\d*(Z|[+-]\d{2}:\d{2})$"""),
        ".$1$2",
    )
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )
    for (pattern in patterns) {
        val parsed = runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = UTC
            }.parse(normalized)
        }.getOrNull()
        if (parsed != null) return parsed.time
    }
    return 0
}

private fun rfc3339Formatter(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = UTC
    }

private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
