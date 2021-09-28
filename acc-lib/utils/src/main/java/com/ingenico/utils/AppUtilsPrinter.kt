package com.ingenico.utils

import android.content.Context
import android.os.RemoteException

class AppUtilsPrinter {
    companion object {

        /** Read `fileName` from this APK assets.  */
        fun readAssetsFile(context: Context, fileName: String): ByteArray? {
            context.assets.open(fileName).use { input ->
                val buffer = ByteArray(input.available())
                val size: Int = input.read(buffer)
                if (size < 0) throw RemoteException("Read failed")
                return buffer
            }
        }

        fun get2FieldsLine(
            maxCharacters : Int,
            title1: String,
            value1: String,
            title2: String,
            value2: String
        ): String? {
            var finalLine: String
            val fill: Int
            val field1 = "$title1 $value1"
            val field2 = "$title2 $value2"
            fill = maxCharacters - field1.length - field2.length
            val blanks = java.lang.StringBuilder()
            var i: Int = 0
            while (i < fill) {
                blanks.append(' ')
                i++
            }
            val sBlanks = blanks.toString()
            finalLine = field1 + sBlanks + field2
            return finalLine
        }
    }
}