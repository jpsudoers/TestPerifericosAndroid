package com.ingenico.acc.workflow

import com.ingenico.acc.R
import com.ingenico.ingp.emv.ActionAnalysisResult
import com.ingenico.ingp.emv.CvmAction

enum class FlowResult{
    EMV_CONTINUE,
    EMV_CANCEL
}

class ResultCode {
    companion object {
        const val SPA_OK = 0
        const val SPA_TIMEOUT = -1
        const val SPA_CANCEL = -2
        const val SPA_RETRY_GET_CARD = -3
        const val SPA_ERROR = -4
    }
}

enum class GetCardEmvErrors(val label: Int){
    EMV_SYSTEM_ERROR(R.string.emv_card_problems),
    EMV_BLOCKED_APP(R.string.emv_blocked_app),
    EMV_NO_APP(R.string.emv_no_app),
    EMV_ACTIVATE_FAIL(R.string.emv_activate_error),
    EMV_POWERUP_FAIL(R.string.emv_no_poweron),
    EMV_REMOVE_CARD(R.string.remove_card),
    EMV_EXCEED_CTLMT(R.string.emv_cless_trx_limit),
    EMV_MISSING_TAG(R.string.emv_missing_param),
    EMV_SHOW_CARD_AGAIN(R.string.emv_show_card_again),
    EMV_DOUBLE_CARD_IN_RANGE(R.string.emv_double_card),
    EMV_KERNEL_ERROR(R.string.emv_kernel_error),
    EMV_SEE_PHONE_INSTRUCTIONS(R.string.emv_see_phone),
    EMV_CARD_NOT_SUPPORTED(R.string.emv_card_not_supported),
    EMV_CLESS_TAP_CARD_AGAIN(R.string.emv_cless_tap_again),
}

data class GetCardResult(
    var entryMode :Int = 0,
    var track1: String = "",
    var track2: String = "",
    var track3: String = "",
    var pan: String = "",
    var last4pan: String = "",
    var expirationDate: String = "",
    var serviceCode: String = "",
    var cardholderName: String = "",
    var PANSequenceNumber: Int = 0,
    var appLabel : String = "",
    var firstCvm : CvmAction? = null
)

data class GoOnChipResult(
    var desicion : ActionAnalysisResult,
    var signature: Int = 0,
    var didOfflinePIN: Int = 0,
    var triesLeft: Int = 0,
    var isBlockedPIN: Int = 0,
    var didOnlinePIN: Int = 0,
    var onlinePINBlock: String = "",
    var pinKsn: String = "",
    var bit55Length : Int = 0,
    var bit55 : String = ""
)

data class FinishChipResult(
    var resultCode : Int = ResultCode.SPA_ERROR,
    var desicion : ActionAnalysisResult = ActionAnalysisResult.AAC,
    val applicationCryptogram: String = "",
    val scriptResult: String = "",
    var tagsLen : Int = 0,
    var tags : String = ""
)

data class OnlinePinResult(
    var onlinePINBlock: String = "",
    var PINKSN: String = ""
)