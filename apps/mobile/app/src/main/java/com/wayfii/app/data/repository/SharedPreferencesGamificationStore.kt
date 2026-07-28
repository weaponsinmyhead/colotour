package com.wayfii.app.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.wayfii.app.data.model.GamificationActivity
import com.wayfii.app.data.model.GamificationActivityType
import com.wayfii.app.data.model.GamificationSyncStatus
import com.wayfii.app.data.model.PlayerProgress
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class LocalActivityRecord(
    val recorded: Boolean,
    val awardedPoints: Int,
    val earnedBadges: List<String>,
    val profile: PlayerProgress,
)

internal interface GamificationLocalStore {
    val playerId: String

    fun progress(syncStatus: GamificationSyncStatus): PlayerProgress

    fun record(
        activity: GamificationActivity,
        syncStatus: GamificationSyncStatus,
    ): LocalActivityRecord

    fun pendingActivities(): List<GamificationActivity>

    fun acknowledge(
        idempotencyKey: String,
        serverProfile: PlayerProgress,
    )

    fun replaceServerProfile(serverProfile: PlayerProgress)
}

@Serializable
internal data class LocalGamificationSnapshot(
    val serverProfile: PlayerProgress = PlayerProgress(
        syncStatus = GamificationSyncStatus.SYNCED,
    ),
    val pendingActivities: List<GamificationActivity> = emptyList(),
    val processedKeys: Set<String> = emptySet(),
)

internal object LocalGamificationLedger {
    fun progress(
        snapshot: LocalGamificationSnapshot,
        syncStatus: GamificationSyncStatus,
    ): PlayerProgress {
        val orderedPending = snapshot.pendingActivities.sortedBy(
            GamificationActivity::occurredAtEpochMillis,
        )
        var points = snapshot.serverProfile.points
        var streak = snapshot.serverProfile.currentStreak
        var lastActivityAt = snapshot.serverProfile.lastActivityAtEpochMillis

        orderedPending.forEach { activity ->
            points += activity.type.points
            streak = nextStreak(
                previousEpochMillis = lastActivityAt,
                currentEpochMillis = activity.occurredAtEpochMillis,
                currentStreak = streak,
            )
            if (activity.occurredAtEpochMillis > lastActivityAt) {
                lastActivityAt = activity.occurredAtEpochMillis
            }
        }

        val badges = LinkedHashSet(snapshot.serverProfile.badges)
        val pendingCounts = orderedPending.groupingBy(GamificationActivity::type).eachCount()
        if (points > 0) {
            badges += "primer_paso"
        }
        if ((pendingCounts[GamificationActivityType.PLACE_VISITED] ?: 0) >= 5) {
            badges += "explorador_local"
        }
        if ((pendingCounts[GamificationActivityType.EVENT_ATTENDED] ?: 0) >= 3) {
            badges += "agenda_viva"
        }
        if (streak >= 7) {
            badges += "racha_7_dias"
        }

        return PlayerProgress(
            points = points,
            level = points / 250 + 1,
            currentStreak = streak,
            badges = badges.toList(),
            lastActivityAtEpochMillis = lastActivityAt,
            pendingSyncCount = snapshot.pendingActivities.size,
            syncStatus = syncStatus,
        )
    }

    fun record(
        snapshot: LocalGamificationSnapshot,
        activity: GamificationActivity,
        syncStatus: GamificationSyncStatus,
    ): Pair<LocalGamificationSnapshot, LocalActivityRecord> {
        val before = progress(snapshot, syncStatus)
        if (activity.idempotencyKey in snapshot.processedKeys) {
            return snapshot to LocalActivityRecord(
                recorded = false,
                awardedPoints = 0,
                earnedBadges = emptyList(),
                profile = before,
            )
        }

        val updated = snapshot.copy(
            pendingActivities = snapshot.pendingActivities + activity,
            processedKeys = snapshot.processedKeys + activity.idempotencyKey,
        )
        val after = progress(updated, syncStatus)
        return updated to LocalActivityRecord(
            recorded = true,
            awardedPoints = activity.type.points,
            earnedBadges = after.badges.filterNot(before.badges::contains),
            profile = after,
        )
    }

    fun acknowledge(
        snapshot: LocalGamificationSnapshot,
        idempotencyKey: String,
        serverProfile: PlayerProgress,
    ): LocalGamificationSnapshot = snapshot.copy(
        serverProfile = serverProfile.copy(
            pendingSyncCount = 0,
            syncStatus = GamificationSyncStatus.SYNCED,
        ),
        pendingActivities = snapshot.pendingActivities.filterNot {
            it.idempotencyKey == idempotencyKey
        },
    )

    private fun nextStreak(
        previousEpochMillis: Long,
        currentEpochMillis: Long,
        currentStreak: Int,
    ): Int {
        if (previousEpochMillis <= 0) return 1
        if (currentEpochMillis < previousEpochMillis) return currentStreak

        val previousDay = utcDay(previousEpochMillis)
        val currentDay = utcDay(currentEpochMillis)
        val dayDifference = (currentDay - previousDay) / MILLIS_PER_DAY
        return when (dayDifference) {
            0L -> currentStreak
            1L -> currentStreak + 1
            else -> 1
        }
    }

    private fun utcDay(epochMillis: Long): Long {
        val calendar = Calendar.getInstance(UTC).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
}

// The boolean result from commit() is intentional: rewards must not be
// reported as saved when durable local persistence failed.
@SuppressLint("UseKtx")
internal class SharedPreferencesGamificationStore(
    context: Context,
) : GamificationLocalStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override val playerId: String
        @Synchronized get() {
            preferences.getString(PLAYER_ID_KEY, null)?.let { return it }
            val generated = "anon_${UUID.randomUUID()}"
            check(preferences.edit().putString(PLAYER_ID_KEY, generated).commit()) {
                "No se pudo guardar la identidad local de Wayfii."
            }
            return generated
        }

    @Synchronized
    override fun progress(syncStatus: GamificationSyncStatus): PlayerProgress =
        LocalGamificationLedger.progress(readSnapshot(), syncStatus)

    @Synchronized
    override fun record(
        activity: GamificationActivity,
        syncStatus: GamificationSyncStatus,
    ): LocalActivityRecord {
        val (updated, result) = LocalGamificationLedger.record(
            snapshot = readSnapshot(),
            activity = activity,
            syncStatus = syncStatus,
        )
        if (result.recorded) {
            writeSnapshot(updated)
        }
        return result
    }

    @Synchronized
    override fun pendingActivities(): List<GamificationActivity> =
        readSnapshot().pendingActivities.sortedBy(GamificationActivity::occurredAtEpochMillis)

    @Synchronized
    override fun acknowledge(
        idempotencyKey: String,
        serverProfile: PlayerProgress,
    ) {
        writeSnapshot(
            LocalGamificationLedger.acknowledge(
                snapshot = readSnapshot(),
                idempotencyKey = idempotencyKey,
                serverProfile = serverProfile,
            ),
        )
    }

    @Synchronized
    override fun replaceServerProfile(serverProfile: PlayerProgress) {
        val snapshot = readSnapshot()
        writeSnapshot(
            snapshot.copy(
                serverProfile = serverProfile.copy(
                    pendingSyncCount = 0,
                    syncStatus = GamificationSyncStatus.SYNCED,
                ),
            ),
        )
    }

    private fun readSnapshot(): LocalGamificationSnapshot {
        val raw = preferences.getString(SNAPSHOT_KEY, null)
            ?: return LocalGamificationSnapshot()
        return try {
            json.decodeFromString(raw)
        } catch (exception: Exception) {
            throw IllegalStateException(
                "No se pudo leer el progreso local de Wayfii.",
                exception,
            )
        }
    }

    private fun writeSnapshot(snapshot: LocalGamificationSnapshot) {
        val saved = preferences.edit()
            .putString(SNAPSHOT_KEY, json.encodeToString(snapshot))
            .commit()
        check(saved) { "No se pudo guardar el progreso local de Wayfii." }
    }

    private companion object {
        const val PREFERENCES_NAME = "wayfii_gamification_v1"
        const val PLAYER_ID_KEY = "anonymous_player_id"
        const val SNAPSHOT_KEY = "gamification_snapshot"
    }
}
