package com.ingenico.acc.workflow

import android.util.Log
import com.ingenico.acc.SecurePayment
import com.ingenico.acc.workflow.model.FinishChipModel
import com.ingenico.ingp.emv.ActionAnalysisResult
import com.ingenico.ingp.emv.EmvSteps
import com.ingenico.ingp.types.hexToByteArray
import com.ingenico.ingp.types.toHexString
import kotlinx.coroutines.runBlocking
import java.util.*

class StepFinishChip(private val securePayment: SecurePayment) {

    private lateinit var emvSteps: EmvSteps
    companion object {
        private val TAG = StepFinishChip::class.java.simpleName
    }

    fun execute(finishChipModel: FinishChipModel): FinishChipResult {

        var finishChipResult : FinishChipResult? = null
        var resultCode = ResultCode.SPA_ERROR

        runBlocking{
            emvSteps = securePayment.getEmvSteps()

            val completionResult = emvSteps.completion(
                onlineAuthorizationResult = finishChipModel.authResponseCode,
                issuerAuthenticationData = finishChipModel.hostF55.hexToByteArray()
            )
            Log.d(TAG, "EmvStep completion result = $completionResult")

            var bit55 = ""

            for(item in finishChipModel.tagList){
                emvSteps.getTagFromKernel(item.toLong(radix = 16))?.toHexString()?.let{
                    if(it.isNotEmpty()){
                        bit55 += item
                        bit55 += "%02x".format(it.length / 2).toUpperCase(Locale.getDefault())
                        bit55 += it
                    }
                }
            }

            emvSteps.stopTransaction()
            securePayment.endTransaction()

            if(completionResult.error == null){
                resultCode = ResultCode.SPA_OK

                finishChipResult = FinishChipResult(
                    resultCode = resultCode,
                    desicion = completionResult.cardDecision!!,
                    applicationCryptogram = completionResult.applicationCryptogram?.toHexString() ?: "",
                    scriptResult = completionResult.scriptResult?.toHexString() ?: "",
                    tags = bit55,
                    tagsLen = bit55.length
                )
            }
            else{
                finishChipResult = FinishChipResult(
                    resultCode = resultCode,
                    desicion = ActionAnalysisResult.AAC,
                    applicationCryptogram = "",
                    scriptResult = ""
                )
            }
        }

        Log.d(TAG, "finishChipResult = $finishChipResult")
        return finishChipResult!!
    }

}