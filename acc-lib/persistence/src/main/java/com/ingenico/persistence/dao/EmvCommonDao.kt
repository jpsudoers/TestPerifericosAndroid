package com.ingenico.persistence.dao

import androidx.room.*
import com.ingenico.persistence.entity.EmvCommonEntity

@Dao
interface EmvCommonDao {

    @Query("SELECT COUNT() FROM emv_common")
    suspend fun getNumRows(): Int

    @Query("SELECT COUNT() FROM emv_common")
    fun getNumRowsJvm(): Int

    @Query("SELECT * FROM emv_common LIMIT 1")
    suspend fun get(): EmvCommonEntity

    @Query("SELECT * FROM emv_common LIMIT 1")
    fun getJvm(): EmvCommonEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(emvCommonEntity: EmvCommonEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setJvm(emvCommonEntity: EmvCommonEntity)

    @Delete
    suspend fun delete(emvCommonEntity: EmvCommonEntity)

    @Delete
    fun deleteJvm(emvCommonEntity: EmvCommonEntity)

    @Query("DELETE FROM emv_common")
    suspend fun deleteAll()

    @Query("DELETE FROM emv_common")
    fun deleteAllJvm()
}