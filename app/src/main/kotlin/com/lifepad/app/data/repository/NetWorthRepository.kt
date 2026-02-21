package com.lifepad.app.data.repository

import com.lifepad.app.data.local.dao.AccountDao
import com.lifepad.app.data.local.dao.AssetDao
import com.lifepad.app.data.local.dao.NetWorthSnapshotDao
import com.lifepad.app.data.local.entity.AssetEntity
import com.lifepad.app.data.local.entity.NetWorthSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetWorthRepository @Inject constructor(
    private val assetDao: AssetDao,
    private val netWorthSnapshotDao: NetWorthSnapshotDao,
    private val accountDao: AccountDao
) {
    fun getAllAssets(): Flow<List<AssetEntity>> = assetDao.getAllAssets()

    fun getAssets(): Flow<List<AssetEntity>> = assetDao.getAssets()

    fun getLiabilities(): Flow<List<AssetEntity>> = assetDao.getLiabilities()

    fun getTotalAssetsValue(): Flow<Double> = assetDao.getTotalAssetsValue()

    fun getTotalLiabilitiesValue(): Flow<Double> = assetDao.getTotalLiabilitiesValue()

    fun getCurrentNetWorth(): Flow<Double> {
        return combine(
            accountDao.getTotalBalance(),
            assetDao.getTotalAssetsValue(),
            assetDao.getTotalLiabilitiesValue()
        ) { accountsTotal, assetsTotal, liabilitiesTotal ->
            (accountsTotal ?: 0.0) + assetsTotal - liabilitiesTotal
        }
    }

    suspend fun getAssetById(id: Long): AssetEntity? = assetDao.getAssetById(id)

    suspend fun saveAsset(asset: AssetEntity): Long {
        val updated = asset.copy(updatedAt = System.currentTimeMillis())
        return assetDao.insert(updated)
    }

    suspend fun deleteAsset(id: Long) = assetDao.deleteById(id)

    // Snapshots
    fun getAllSnapshots(): Flow<List<NetWorthSnapshotEntity>> = netWorthSnapshotDao.getAllSnapshots()

    fun getSnapshotsForPeriod(startDate: Long, endDate: Long): Flow<List<NetWorthSnapshotEntity>> =
        netWorthSnapshotDao.getSnapshotsForPeriod(startDate, endDate)

    suspend fun takeMonthlySnapshotIfNeeded() {
        val monthStart = getMonthStart()
        val latest = netWorthSnapshotDao.getLatestSnapshot()

        // Only take one snapshot per month
        if (latest != null && getMonthStart(latest.snapshotDate) == monthStart) return

        val accountsTotal = accountDao.getTotalBalance().firstOrNull() ?: 0.0
        val assetsTotal = assetDao.getTotalAssetsValue().firstOrNull() ?: 0.0
        val liabilitiesTotal = assetDao.getTotalLiabilitiesValue().firstOrNull() ?: 0.0

        val snapshot = NetWorthSnapshotEntity(
            snapshotDate = monthStart,
            accountsTotal = accountsTotal,
            assetsTotal = assetsTotal,
            liabilitiesTotal = liabilitiesTotal,
            netWorth = accountsTotal + assetsTotal - liabilitiesTotal
        )
        netWorthSnapshotDao.upsertSnapshot(snapshot)
    }

    private fun getMonthStart(timeMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            this.timeInMillis = timeMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
