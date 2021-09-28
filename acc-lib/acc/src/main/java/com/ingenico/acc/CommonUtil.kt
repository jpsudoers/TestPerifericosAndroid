package com.ingenico.acc

import android.util.Log
import com.ingenico.ingp.emv.PinEntryStatus
import com.ingenico.ingp.types.toHexString

internal fun com.ingenico.ingp.dev.security.PinEntryStatus.toEmvPinEntryStatus() = when (this) {
    com.ingenico.ingp.dev.security.PinEntryStatus.CANCEL -> PinEntryStatus.ENTRY_CANCEL
    com.ingenico.ingp.dev.security.PinEntryStatus.PIN_AVAILABLE -> PinEntryStatus.ENTRY_SUCCESS
    com.ingenico.ingp.dev.security.PinEntryStatus.PIN_BYPASS -> PinEntryStatus.ENTRY_BYPASS
    com.ingenico.ingp.dev.security.PinEntryStatus.TIMEOUT -> PinEntryStatus.ENTRY_TIMEOUT
    else -> PinEntryStatus.ENTRY_ERROR
}

internal fun logEmvTag(tag : String, label : String, emvTagTmTvr: String) {
    Log.d(tag, "Tag $emvTagTmTvr - $label: ${SecurePayment.getEmvSteps().getTagFromKernel(
            emvTagTmTvr.toLong(radix = 16))?.toHexString()}")
}