package com.sipl.egs2.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sipl.egs2.database.entity.Labour
import com.sipl.egs2.database.model.LabourWithAreaNames

@Dao
interface LabourDao {
    @Insert
    suspend fun insertLabour(labour: Labour) : Long

    @Update
    suspend fun updateLabour(labour: Labour):Int

    @Delete
    suspend fun deleteLabour(labour: Labour)

    @Query("SELECT * FROM labours WHERE isSynced='0'")
    fun getAllLabour(): List<Labour>

    @Query("SELECT * FROM labours WHERE id = :id")
    suspend fun getLabourById(id: Int): Labour

    @Query("SELECT * FROM labours WHERE mgnregaId = :mgnregaId")
    suspend fun getLabourByMgnregaId(mgnregaId: String): Labour

    @Query("SELECT * FROM labours WHERE mgnregaId like '%' || :searchQuery || '%'")
    suspend fun getLabourByMgnregaIdLike(searchQuery: String): List<Labour>

    @Query("SELECT COUNT(*) FROM labours WHERE isSynced='0'")
    suspend fun getLaboursCount(): Int

    @Query("SELECT l.*, village.name AS villageName, district.name AS districtName, taluka.name AS talukaName " +
            "FROM labours l " +
            "LEFT JOIN area AS village ON l.village = village.location_id " +
            "LEFT JOIN area AS district ON l.district = district.location_id " +
            "LEFT JOIN area AS taluka ON l.taluka = taluka.location_id WHERE isSynced='0' ORDER BY id DESC")
    suspend fun getLabourWithAreaNames(): List<LabourWithAreaNames>


    @Query("SELECT l.*, village.name AS villageName, district.name AS districtName, taluka.name AS talukaName, gender.gender_name AS genderName, skills.skills AS skillName " +
            "FROM labours l " +
            "LEFT JOIN area AS village ON l.village = village.location_id " +
            "LEFT JOIN area AS district ON l.district = district.location_id " +
            "LEFT JOIN area AS taluka ON l.taluka = taluka.location_id " +
            "LEFT JOIN gender ON l.gender = gender.id " + // Join with the gender table
            "LEFT JOIN skills ON l.skill = skills.id " + // Join with the skills table
            "WHERE l.id = :labourId AND isSynced='0'")
    suspend fun getLabourWithAreaNamesById(labourId: Int): LabourWithAreaNames?
}