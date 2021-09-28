package com.ingenico.persistence.dao

import androidx.room.*
import com.ingenico.persistence.entity.EmvCAKeyEntity

@Dao
interface EmvCAkeyDao {

    @Query("SELECT COUNT() FROM emv_cakey")
    suspend fun getNumRows(): Int

    @Query("SELECT COUNT() FROM emv_cakey")
    fun getNumRowsJvm(): Int

    @Query("SELECT * FROM emv_cakey")
    suspend fun getAll(): MutableList<EmvCAKeyEntity>

    @Query("SELECT * FROM emv_cakey")
    fun getAllJvm(): MutableList<EmvCAKeyEntity>

    @Query("SELECT * FROM emv_cakey WHERE id = :id")
    suspend fun getById(id : Int): EmvCAKeyEntity

    @Query("SELECT * FROM emv_cakey WHERE id = :id")
    fun getByIdJvm(id : Int): EmvCAKeyEntity

    @Query("SELECT * FROM emv_cakey WHERE rid = :rid")
    suspend fun getByRid(rid : String): MutableList<EmvCAKeyEntity>

    @Query("SELECT * FROM emv_cakey WHERE rid = :rid")
    fun getByRidJvm(rid : String): MutableList<EmvCAKeyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emvCAkeyEntity: EmvCAKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertJvm(emvCAkeyEntity: EmvCAKeyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArray(arrEmvCAkeyEntity: MutableList<EmvCAKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertArrayJvm(arrEmvCAkeyEntity: MutableList<EmvCAKeyEntity>)

    @Delete
    suspend fun delete(emvCAkeyEntity: EmvCAKeyEntity)

    @Delete
    fun deleteJvm(emvCAkeyEntity: EmvCAKeyEntity)

    @Query("DELETE FROM emv_cakey")
    suspend fun deleteAll()

    @Query("DELETE FROM emv_cakey")
    fun deleteAllJvm()
}