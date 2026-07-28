package com.wayfii.app.data.repository

import com.wayfii.app.data.model.GamificationActivity
import com.wayfii.app.data.model.GamificationActivityType
import com.wayfii.app.data.model.GamificationSyncStatus
import com.wayfii.app.data.model.PlayerProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalGamificationLedgerTest {

    @Test
    fun `local ledger is idempotent and exposes pending reward immediately`() {
        val activity = visit(index = 1)
        val (updated, first) = LocalGamificationLedger.record(
            snapshot = LocalGamificationSnapshot(),
            activity = activity,
            syncStatus = GamificationSyncStatus.LOCAL_ONLY,
        )

        assertTrue(first.recorded)
        assertEquals(20, first.awardedPoints)
        assertEquals(20, first.profile.points)
        assertEquals(1, first.profile.pendingSyncCount)
        assertEquals(listOf("primer_paso"), first.earnedBadges)

        val (unchanged, duplicate) = LocalGamificationLedger.record(
            snapshot = updated,
            activity = activity,
            syncStatus = GamificationSyncStatus.LOCAL_ONLY,
        )

        assertFalse(duplicate.recorded)
        assertEquals(0, duplicate.awardedPoints)
        assertEquals(updated, unchanged)
        assertEquals(20, duplicate.profile.points)
    }

    @Test
    fun `acknowledging an activity folds it into the server profile`() {
        val (pending, _) = LocalGamificationLedger.record(
            snapshot = LocalGamificationSnapshot(),
            activity = visit(index = 1),
            syncStatus = GamificationSyncStatus.PENDING,
        )

        val acknowledged = LocalGamificationLedger.acknowledge(
            snapshot = pending,
            idempotencyKey = "visit-1",
            serverProfile = PlayerProgress(
                points = 20,
                level = 1,
                currentStreak = 1,
                badges = listOf("primer_paso"),
                lastActivityAtEpochMillis = DAY_ONE,
                syncStatus = GamificationSyncStatus.SYNCED,
            ),
        )
        val progress = LocalGamificationLedger.progress(
            snapshot = acknowledged,
            syncStatus = GamificationSyncStatus.SYNCED,
        )

        assertTrue(acknowledged.pendingActivities.isEmpty())
        assertEquals(20, progress.points)
        assertEquals(0, progress.pendingSyncCount)
        assertEquals(GamificationSyncStatus.SYNCED, progress.syncStatus)
    }

    @Test
    fun `five local visits unlock explorer badge`() {
        var snapshot = LocalGamificationSnapshot()
        repeat(5) { index ->
            snapshot = LocalGamificationLedger.record(
                snapshot = snapshot,
                activity = visit(index = index + 1),
                syncStatus = GamificationSyncStatus.LOCAL_ONLY,
            ).first
        }

        val progress = LocalGamificationLedger.progress(
            snapshot = snapshot,
            syncStatus = GamificationSyncStatus.LOCAL_ONLY,
        )
        assertEquals(100, progress.points)
        assertTrue("explorador_local" in progress.badges)
    }

    private fun visit(index: Int): GamificationActivity = GamificationActivity(
        idempotencyKey = "visit-$index",
        type = GamificationActivityType.PLACE_VISITED,
        subjectId = "place-$index",
        occurredAtEpochMillis = DAY_ONE + index * 1_000L,
    )

    private companion object {
        const val DAY_ONE = 1_785_235_200_000L
    }
}
