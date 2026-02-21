package com.lifepad.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepad.app.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY assetType ASC, name ASC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE isLiability = 0 ORDER BY assetType ASC, name ASC")
    fun getAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE isLiability = 1 ORDER BY assetType ASC, name ASC")
    fun getLiabilities(): Flow<List<AssetEntity>>

    @Query("SELECT COALESCE(SUM(value), 0.0) FROM assets WHERE isLiability = 0")
    fun getTotalAssetsValue(): Flow<Double>

    @Query("SELECT COALESCE(SUM(value), 0.0) FROM assets WHERE isLiability = 1")
    fun getTotalLiabilitiesValue(): Flow<Double>

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: Long): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
