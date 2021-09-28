package com.ingenico.acc.workflow.model

import com.ingenico.ingp.emv.kernel.ServiceType
import com.ingenico.ingp.types.Amount
import com.ingenico.ingp.types.emv.TransactionType

data class GetCardModel (
    var supportSwipe: Boolean = true,
    var supportChip: Boolean = true,
    var supportCless: Boolean = true,
    var timeoutSearchCard: Int = 30,
    var serviceType : ServiceType,
    var amount: Amount,
    var transactionType: TransactionType,
    var date: String,
    var time: String,
    var sequenceCounter: Int,
    var otherAmount: Amount
)