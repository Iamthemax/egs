package com.sipl.egs2.database.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sipl.egs2.database.entity.AreaItem

@Dao
interface AreaDao {
    @Query("SELECT * FROM area WHERE is_active=1 ORDER BY name ASC ")
    suspend fun getAllArea(): List<AreaItem>

    @Query("SELECT COUNT(*) FROM area WHERE is_active=1")
    suspend fun getRowCount(): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<AreaItem>)
    @Query("SELECT * FROM area WHERE location_type=2 AND is_active=1 ORDER BY name ASC")
    suspend fun getAllDistrict(): List<AreaItem>

    @Query("SELECT * FROM area WHERE location_type=3  AND  parent_id = :location_id AND is_active=1 ORDER BY name ASC")
    suspend fun getAllTalukas(location_id: String): List<AreaItem>

    @Query("SELECT * FROM area WHERE location_type=4  AND  parent_id = :location_id  AND is_active=1 ORDER BY name ASC")
    suspend fun getVillageByTaluka(location_id: String): List<AreaItem>

    @Query("SELECT * FROM area WHERE location_id = :location_id AND is_active=1  ORDER BY name ASC")
    suspend fun getAreaByLocationId(location_id: String): AreaItem

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AreaItem)

    @Update
    suspend fun update(entity: AreaItem)





    @Transaction
    suspend fun insertInitialRecords(items: List<AreaItem>) {

        if (getAllArea().isEmpty()) {
            insertAll(items)
        }else{
            Log.d("mytag","Area is not empty")
        }
    }


}