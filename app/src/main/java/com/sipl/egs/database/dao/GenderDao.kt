package com.sipl.egs.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sipl.egs.database.entity.Gender

@Dao
interface GenderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Gender>)

    @Query("DELETE FROM gender")
    suspend fun deleteAllGenders()

    @Query("SELECT COUNT(*) FROM gender")
    suspend fun getRowCount(): Int
    @Transaction
    suspend fun insertInitialRecords(items: List<Gender>) {
        deleteAllGenders()
        insertAll(items)
    }

    @Query("SELECT * FROM gender ORDER BY id ASC")
    suspend fun getAllGenders(): List<Gender>

    @Query("SELECT * FROM gender WHERE id = :id")
    suspend fun getGenderById(id: String): Gender

}