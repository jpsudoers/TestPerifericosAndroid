package com.ingenico.acc.workflow

object Transaction {
    const val MODE_NONE = 0
    const val MODE_MANUAL = 1
    const val MODE_MAG = 2
    const val MODE_CHIP = 3
    const val MODE_CTLS = 4
    const val MODE_CTLS_MS = 5

    const val DECISION_APPROVAL = 0
    const val DECISION_DENIAL = 1
    const val DECISION_ONLINE = 2
}