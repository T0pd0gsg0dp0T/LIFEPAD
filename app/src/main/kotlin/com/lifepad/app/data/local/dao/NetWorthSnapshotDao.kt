package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepad.app.data.local.entity.NetWorthSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthSnapshotDao {
    @Query("SELECT * FROM net_worth_snapshots ORDER BY snapshotDate ASC")
    fun getAllSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots WHERE snapshotDate >= :startDate AND snapshotDate <= :endDate ORDER BY snapshotDate ASC")
    fun getSnapshotsForPeriod(startDate: Long, endDate: Long): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots ORDER BY snapshotDate DESC LIMIT 1")
    suspend fun getLatestSnapshot(): NetWorthSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: NetWorthSnapshotEntity): Long

    @Query("DELETE FROM net_worth_snapshots WHERE snapshotDate < :beforeDate")
    suspend fun deleteOldSnapshots(beforeDate: Long)
}
