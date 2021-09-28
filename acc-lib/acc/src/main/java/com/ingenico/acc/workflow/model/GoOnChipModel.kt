package com.ingenico.acc.workflow.model

import com.ingenico.acc.EnumKeyAlgorithm

data class GoOnChipModel (
    val pinKeyId: String, //format ACC -> "0000 00",
    val pinAlgorithm: EnumKeyAlgorithm = EnumKeyAlgorithm.TDES,
    val pinTimeout: Int,
    var tagList: MutableList<String>
)