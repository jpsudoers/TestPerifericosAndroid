package com.ingenico.persistence.dao

import androidx.room.*
import com.ingenico.persistence.entity.EmvAidEntity

@Dao
interface EmvAidDao {

    @Query("SELECT COUNT() FROM emv_aid")
    suspend fun getNumRows(): Int

    @Query("SELECT COUNT() FROM emv_aid")
    fun getNumRowsJvm(): Int

    @Query("SELECT * FROM emv_aid")
    suspend fun getAll(): MutableList<EmvAidEntity>

    @Query("SELECT * FROM emv_aid")
    fun getAllJvm(): MutableList<EmvAidEntity>

    @Query("SELECT * FROM emv_aid WHERE id = :id")
    suspend fun getById(id : Int): EmvAidEntity

    @Query("SELECT * FROM emv_aid WHERE id = :id")
    fun getByIdJvm(id : Int): EmvAidEntity

    @Query("SELECT * FROM emv_aid WHERE AID LIKE :aid || '%' AND technology =:technology")
    suspend fun getByAid(aid : String, technology: Int): EmvAidEntity?

    @Query("SELECT * FROM emv_aid WHERE AID LIKE :aid || '%' AND technology =:technology")
    fun getByAidJvm(aid : String, technology: Int): EmvAidEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emvAidEntity: EmvAidEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertJvm(emvAidEntity: EmvAidEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArray(arrEmvAidEntity: MutableList<EmvAidEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertArrayJvm(arrEmvAidEntity: MutableList<EmvAidEntity>)

    @Delete
    suspend fun delete(emvAidEntity: EmvAidEntity)

    @Delete
    fun deleteJvm(emvAidEntity: EmvAidEntity)

    @Query("DELETE FROM emv_aid")
    suspend fun deleteAll()

    @Query("DELETE FROM emv_aid")
    fun deleteAllJvm()
}