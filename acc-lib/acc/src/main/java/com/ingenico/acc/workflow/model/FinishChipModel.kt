package com.ingenico.acc.workflow.model

data class FinishChipModel (
    var statusOnline : Int = 1,
    var authResponseCode : String = "",
    var hostF55Len : Int = 0,
    var hostF55 : String = "",
    var tagList: MutableList<String>
)