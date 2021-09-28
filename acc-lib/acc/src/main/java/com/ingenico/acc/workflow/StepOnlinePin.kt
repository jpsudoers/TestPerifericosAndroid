package com.ingenico.acc.workflow

import android.util.Log
import com.ingenico.acc.SecurePayment
import com.ingenico.acc.toEmvPinEntryStatus
import com.ingenico.acc.workflow.model.OnlinePinModel
import com.ingenico.ingp.emv.PinEntryStatus
import com.ingenico.ingp.types.toHexString
import kotlinx.coroutines.*

interface IStepOnlinePin {
    fun onOutputResult(result: Int, onlinePinResult: OnlinePinResult?)
}

class StepOnlinePin(private val securePayment: SecurePayment, private val iStepOnlinePin: IStepOnlinePin) {

    private var job : Job? = null
    companion object {
        private val TAG = StepOnlinePin::class.java.simpleName
    }

    fun execute(onlinePinModel: OnlinePinModel){
        job = CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Default) {
                try {
                    Log.d(TAG, "execute: $onlinePinModel")

                    val pinEntryResult = securePayment.startPinEntryOnline(
                            onlinePinModel.keyId,
                            onlinePinModel.algorithm,
                            onlinePinModel.pan,
                            onlinePinModel.timeoutSec
                    )

                    val pinEntryStatus = pinEntryResult.status.toEmvPinEntryStatus()
                    Log.d(TAG, "pinEntryStatus $pinEntryStatus")

                    if (pinEntryStatus == PinEntryStatus.ENTRY_CANCEL
                            || pinEntryStatus == PinEntryStatus.ENTRY_TIMEOUT) {
                        iStepOnlinePin.onOutputResult(ResultCode.SPA_CANCEL, null)
                        rebootTransaction()
                        return@withContext
                    }

                    val onlinePinResult = OnlinePinResult(
                            onlinePINBlock = pinEntryResult.pinBlock?.toHexString() ?: "",
                            PINKSN = securePayment.getKSN(onlinePinModel.keyId)
                    )

                    Log.d(TAG, "onlinePinResult $onlinePinResult")
                    iStepOnlinePin.onOutputResult(ResultCode.SPA_OK, onlinePinResult)
                }
                catch (ex : Exception){
                    Log.d(TAG, "Exception : Line[${ex.stackTrace[0].lineNumber}] message: $ex")
                }
            }
        }

    }

    private fun rebootTransaction(){
        securePayment.endTransaction()
    }
}