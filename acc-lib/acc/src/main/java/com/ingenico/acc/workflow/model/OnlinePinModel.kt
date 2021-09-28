package com.ingenico.acc.workflow.model

import com.ingenico.acc.EnumKeyAlgorithm

data class OnlinePinModel (
    val keyId: String, //format ACC -> "0000 00",
    val algorithm: EnumKeyAlgorithm = EnumKeyAlgorithm.TDES,
    val pan: String,
    val timeoutSec : Int
)