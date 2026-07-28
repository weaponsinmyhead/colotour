package com.wayfii.app.data.repository

import com.wayfii.app.data.model.ActivityVisualType
import com.wayfii.app.data.model.GamificationActivity
import com.wayfii.app.data.model.GamificationActivityType
import com.wayfii.app.data.model.GamificationSyncStatus
import com.wayfii.app.data.model.ItineraryStop
import com.wayfii.app.data.model.PlayerProgress
import com.wayfii.app.data.model.RewardReceipt
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class OfflineFirstGamificationRepository(
    private val localStore: GamificationLocalStore,
    private val remote: GamificationRemoteDataSource?,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : GamificationRepository {

    override suspend fun loadLocalProgress(): PlayerProgress = withContext(Dispatchers.IO) {
        localStore.progress(localStatus())
    }

    override suspend fun recordStopCompleted(
        adventureSessionId: String,
        destination: String,
        stop: ItineraryStop,
    ): RewardReceipt {
        val activityType = if (stop.visualType == ActivityVisualType.EVENT) {
            GamificationActivityType.EVENT_ATTENDED
        } else {
            GamificationActivityType.PLACE_VISITED
        }
        val subjectId = stop.placeId?.trim()?.takeIf(String::isNotEmpty)
            ?: "local-place-${digest("$adventureSessionId|${stop.order}|${stop.titulo}").take(24)}"
        return record(
            type = activityType,
            subjectId = subjectId,
            adventureSessionId = adventureSessionId,
            metadata = mapOf(
                "destination" to destination.safeMetadata(),
                "stopTitle" to stop.titulo.safeMetadata(),
                "stopOrder" to stop.order.toString(),
            ),
        )
    }

    override suspend fun recordItineraryCompleted(
        adventureSessionId: String,
        destination: String,
    ): RewardReceipt = record(
        type = GamificationActivityType.ITINERARY_COMPLETED,
        subjectId = "itinerary-${digest(adventureSessionId).take(24)}",
        adventureSessionId = adventureSessionId,
        metadata = mapOf(
            "destination" to destination.safeMetadata(),
        ),
    )

    override suspend fun syncProgress(): PlayerProgress = withContext(Dispatchers.IO) {
        val remoteSource = remote
            ?: return@withContext localStore.progress(GamificationSyncStatus.LOCAL_ONLY)

        for (activity in localStore.pendingActivities().take(MAX_SYNC_BATCH_SIZE)) {
            val result = remoteSource.recordActivity(localStore.playerId, activity)
                .getOrElse {
                    return@withContext localStore.progress(GamificationSyncStatus.PENDING)
                }
            localStore.acknowledge(
                idempotencyKey = activity.idempotencyKey,
                serverProfile = result.profile,
            )
        }

        if (localStore.pendingActivities().isNotEmpty()) {
            return@withContext localStore.progress(GamificationSyncStatus.PENDING)
        }

        remoteSource.getPlayer(localStore.playerId)
            .onSuccess(localStore::replaceServerProfile)

        localStore.progress(GamificationSyncStatus.SYNCED)
    }

    private suspend fun record(
        type: GamificationActivityType,
        subjectId: String,
        adventureSessionId: String,
        metadata: Map<String, String>,
    ): RewardReceipt = withContext(Dispatchers.IO) {
        val occurredAt = nowMillis()
        val idempotencyKey = "mobile-${digest(
            "${localStore.playerId}|${type.name}|$subjectId|${utcDate(occurredAt)}",
        )}"
        val localRecord = localStore.record(
            activity = GamificationActivity(
                idempotencyKey = idempotencyKey,
                type = type,
                subjectId = subjectId,
                occurredAtEpochMillis = occurredAt,
                metadata = metadata + (
                    "adventureSession" to digest(adventureSessionId).take(24)
                    ),
            ),
            syncStatus = localStatus(),
        )
        RewardReceipt(
            recorded = localRecord.recorded,
            awardedPoints = localRecord.awardedPoints,
            earnedBadges = localRecord.earnedBadges,
            profile = localRecord.profile,
        )
    }

    private fun localStatus(): GamificationSyncStatus = if (remote == null) {
        GamificationSyncStatus.LOCAL_ONLY
    } else {
        val hasPending = localStore.pendingActivities().isNotEmpty()
        if (hasPending) GamificationSyncStatus.PENDING else GamificationSyncStatus.SYNCED
    }

    private fun utcDate(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = UTC
        }.format(Date(epochMillis))

    private fun digest(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun String.safeMetadata(): String = trim().take(MAX_METADATA_VALUE_LENGTH)

    private companion object {
        const val MAX_SYNC_BATCH_SIZE = 50
        const val MAX_METADATA_VALUE_LENGTH = 200
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    }
}
