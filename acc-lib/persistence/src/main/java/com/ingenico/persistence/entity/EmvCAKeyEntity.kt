package com.ingenico.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emv_cakey")
data class EmvCAKeyEntity(

    @PrimaryKey(autoGenerate = true)
    val uid : Int = 0,

    @ColumnInfo(name = "id")
    val id : Int = 0,

    @ColumnInfo(name = "nameBrand")
    val nameBrand : String = "",

    @ColumnInfo(name = "rid")
    val rid : String = "",

    @ColumnInfo(name = "index")
    val index : String = "",

    @ColumnInfo(name = "exponent")
    val exponent : String = "",

    @ColumnInfo(name = "expirationDate")
    val expirationDate : String = "",

    @ColumnInfo(name = "keyValue")
    val keyValue : String = ""



)