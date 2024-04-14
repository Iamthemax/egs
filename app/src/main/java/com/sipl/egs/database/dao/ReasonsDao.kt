package com.sipl.egs.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sipl.egs.database.entity.Reasons


@Dao
interface ReasonsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Reasons>)

    @Query("DELETE FROM reasons")
    suspend fun deleteAllReasons()
    @Transaction
    suspend fun insertInitialRecords(items: List<Reasons>) {
        deleteAllReasons()
        insertAll(items)
    }
    @Query("SELECT COUNT(*) FROM reasons")
    suspend fun getRowCount(): Int
    @Query("SELECT * FROM reasons ORDER BY id ASC")
    suspend fun getAllReasons(): List<Reasons>
    @Query("SELECT * FROM reasons WHERE id = :id")
    suspend fun getReasonById(id: String): Reasons
}