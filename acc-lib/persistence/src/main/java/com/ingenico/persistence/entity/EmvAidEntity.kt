package com.ingenico.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emv_aid")
data class EmvAidEntity (

    @PrimaryKey(autoGenerate = true)
    val uid : Int = 0,

    @ColumnInfo(name = "id")
    val id : Int = 0,

    @ColumnInfo(name = "termCapabilities")
    val termCapabilities : String = "",

    @ColumnInfo(name = "termAddCapabilities")
    val termAddCapabilities: String = "",

    @ColumnInfo(name = "ttq")
    val ttq : String = "",

    @ColumnInfo(name = "name")
    val name : String = "",

    @ColumnInfo(name = "AID")
    val AID : String = "",

    @ColumnInfo(name = "debitFlag")
    val debitFlag : Int = 0,

    @ColumnInfo(name = "technology")
    val technology : Int = 0,

    @ColumnInfo(name = "appVersion1")
    val appVersion1 : String = "",

    @ColumnInfo(name = "appVersion2")
    val appVersion2 : String = "",

    @ColumnInfo(name = "appVersion3")
    val appVersion3 : String = "",

    @ColumnInfo(name = "appVersion4")
    val appVersion4 : String = "",

    @ColumnInfo(name = "tacDefault")
    val tacDefault : String = "",

    @ColumnInfo(name = "tacDenial")
    val tacDenial : String = "",

    @ColumnInfo(name = "tacOnline")
    val tacOnline : String = "",

    @ColumnInfo(name = "emvTDOL")
    val emvTDOL : String = "000000000000",

    @ColumnInfo(name = "emvDDOL")
    val emvDDOL : String = "000000000000",

    @ColumnInfo(name = "emvFloorLimit")
    val emvFloorLimit : Long = 0,

    @ColumnInfo(name = "clessFloorLimit")
    val clessFloorLimit : Long = 0,

    @ColumnInfo(name = "clessCvmLimit")
    val clessCvmLimit : Long = 0,

    @ColumnInfo(name = "clessTransactionLimit")
    val clessTransactionLimit : Long = 0
)