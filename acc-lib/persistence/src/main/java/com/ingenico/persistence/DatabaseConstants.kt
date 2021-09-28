package com.ingenico.persistence

class DatabaseConstants {
    companion object {
        const val DB_NAME = "acc_lib_db"
        const val DB_VERSION = 2

        const val DB_EMV_CONTACT = 1
        const val DB_EMV_PAYWAVE = 3
        const val DB_EMV_PAYPASS = 5
        const val DB_EMV_EXPRESSPAY = 7
        const val DB_EMV_DISCOVER_DPAS = 13
        const val DB_EMV_DISCOVER_ZIP = 14
        const val DB_EMV_JCB = 16
    }
}