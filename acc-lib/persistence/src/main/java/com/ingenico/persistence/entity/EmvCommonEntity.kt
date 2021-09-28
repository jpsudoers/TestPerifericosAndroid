package com.ingenico.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emv_common")
data class EmvCommonEntity(

    @PrimaryKey(autoGenerate = true)
    val uid : Int = 0,

    @ColumnInfo(name = "termType")
    val termType : String = "",

    @ColumnInfo(name = "termCountryCode")
    val termCountryCode : String = "",

    @ColumnInfo(name = "termCapabilities")
    val termCapabilities : String = "",

    @ColumnInfo(name = "termAddCapabilities")
    val termAddCapabilities: String = "",

    @ColumnInfo(name = "tacDefault")
    val tacDefault: String = "",

    @ColumnInfo(name = "tacDenial")
    val tacDenial: String = "",

    @ColumnInfo(name = "tacOnline")
    val tacOnline: String = "",

    @ColumnInfo(name = "applicationVersionNumber")
    val applicationVersionNumber: String = "",

    @ColumnInfo(name = "biasedSelectionThreshold")
    val biasedSelectionThreshold: Long = 0,

    @ColumnInfo(name = "targetPercentage")
    val targetPercentage: Int = 0,

    @ColumnInfo(name = "targetMaxPercentage")
    val targetMaxPercentage: Int = 0,

    @ColumnInfo(name = "readerConfigurationParameters")
    val readerConfigurationParameters: String = "7B00",

    @ColumnInfo(name = "terminalInterchangeProfile")
    val terminalInterchangeProfile: String = "708000",

    @ColumnInfo(name = "amexCtlReaderCapabilities")
    val amexCtlReaderCapabilities: String = "C8",

    @ColumnInfo(name = "amexEnhancedCtlReaderCapabilities")
    val amexEnhancedCtlReaderCapabilities: String = "D8E00000"
)