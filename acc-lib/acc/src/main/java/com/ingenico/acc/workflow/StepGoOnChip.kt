package com.ingenico.acc.workflow

import android.util.Log
import com.ingenico.acc.SecurePayment
import com.ingenico.acc.logEmvTag
import com.ingenico.acc.toEmvPinEntryStatus
import com.ingenico.acc.workflow.model.GoOnChipModel
import com.ingenico.ingp.dev.security.PinEntryResult
import com.ingenico.ingp.emv.*
import com.ingenico.ingp.types.toHexString
import com.usdk.apiservice.aidl.emv.EMVTag.*
import kotlinx.coroutines.*
import java.util.*


interface IStepGoOnChip {

    fun onOutputResult(result: Int, goOnChipResult: GoOnChipResult?)
}

class StepGoOnChip(private val securePayment: SecurePayment, private val iStepGoOnChip: IStepGoOnChip) {

    private var job : Job? = null
    private lateinit var emvSteps: EmvSteps
    companion object {
        private val TAG = StepGoOnChip::class.java.simpleName
    }

    fun execute(goOnChipModel: GoOnChipModel){
        job = CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Default) {
                try {
                    Log.d(TAG, "execute: $goOnChipModel")

                    var pinEntryStatus: PinEntryStatus?
                    var signature = 0
                    var didOfflinePIN = 0
                    var didOnlinePIN = 0
                    var pinEntryResult: PinEntryResult? = null
                    var cvmStep = SecurePayment.firstCvm
                    var cvmResult: CvmResult?

                    emvSteps = securePayment.getEmvSteps()
                    logEmvTags()
                    Log.d(TAG, "firstCvm = $cvmStep")

                    loop@ do {

                        when (cvmStep) {
                            CvmAction.ONLINE_PIN -> {

                                didOnlinePIN = 1
                                pinEntryResult = securePayment.startPinEntryOnline(
                                        goOnChipModel.pinKeyId,
                                        goOnChipModel.pinAlgorithm,
                                        emvSteps.getPan()!!.value,
                                        goOnChipModel.pinTimeout
                                )

                                pinEntryStatus = pinEntryResult.status.toEmvPinEntryStatus()
                                Log.d(TAG, "pinEntryStatus $pinEntryStatus")

                                if (pinEntryStatus == PinEntryStatus.ENTRY_CANCEL
                                        || pinEntryStatus == PinEntryStatus.ENTRY_TIMEOUT) {
                                    iStepGoOnChip.onOutputResult(ResultCode.SPA_CANCEL, null)
                                    rebootTransaction()
                                    return@withContext
                                } else {
                                    cvmResult = emvSteps.cardholderVerification(
                                            pinEntryStatus = pinEntryStatus
                                    )
                                }
                            }
                            CvmAction.OFFLINE_PIN -> {
                                didOfflinePIN = 1
                                cvmResult = securePayment.startPinEntryOffline()
                            }
                            CvmAction.SIGNATURE -> {
                                signature = 1
                                break@loop
                            }
                            else -> {
                                break@loop
                            }
                        }

                        if (cvmResult != null) {
                            if (cvmResult.nextCvm == CvmAction.ONLINE_PIN)
                                break@loop
                            else{
                                if(cvmResult.nextCvm != null)
                                    cvmStep = cvmResult.nextCvm!!
                                else
                                    break@loop
                            }
                        } else {
                            iStepGoOnChip.onOutputResult(ResultCode.SPA_CANCEL, null)
                            rebootTransaction()
                            return@withContext
                        }

                        Log.d(TAG, "nextCvm = ${cvmResult.nextCvm}")

                    } while (cvmResult?.nextCvm != null && (cvmResult.nextCvm != CvmAction.END && cvmResult.nextCvm != CvmAction.NO_CVM))

                    val riskManagementResult = emvSteps.riskManagement()
                    Log.d(TAG, "EmvStep riskManagement result = $riskManagementResult")

                    if (riskManagementResult.error != null) {
                        iStepGoOnChip.onOutputResult(ResultCode.SPA_ERROR, null)
                        rebootTransaction()
                        return@withContext
                    }

                    var bit55 = ""

                    for (item in goOnChipModel.tagList) {
                        emvSteps.getTagFromKernel(item.toLong(radix = 16))?.toHexString()?.let {
                            if (it.isNotEmpty()) {
                                bit55 += item
                                bit55 += "%02x".format(it.length / 2).toUpperCase(Locale.getDefault())
                                bit55 += it
                            }
                        }
                    }

                    val ksn = if (pinEntryResult?.pinBlock?.toHexString() != null)
                        securePayment.getKSN(goOnChipModel.pinKeyId)
                    else
                        ""

                    val goOnChipResult = GoOnChipResult(
                            desicion = riskManagementResult.cardDecision!!,
                            signature = signature,
                            didOfflinePIN = didOfflinePIN,
                            triesLeft = 0,
                            isBlockedPIN = 0,
                            didOnlinePIN = didOnlinePIN,
                            onlinePINBlock = pinEntryResult?.pinBlock?.toHexString() ?: "",
                            pinKsn = ksn,
                            bit55 = bit55,
                            bit55Length = bit55.length
                    )

                    Log.d(TAG, "goOnChipResult $goOnChipResult")
                    iStepGoOnChip.onOutputResult(ResultCode.SPA_OK, goOnChipResult)
                }
                catch (ex : Exception){
                    Log.d(TAG, "Exception : Line[${ex.stackTrace[0].lineNumber}] message: $ex")
                    iStepGoOnChip.onOutputResult(ResultCode.SPA_ERROR, null)
                }
            }
        }
    }

    fun stop(){
        Log.d(TAG, "StepGoOnChip stop - job Cancel request")
        job?.cancel()
        Log.d(TAG, "StepGoOnChip stop - securePayment endTransaction")
        securePayment.endTransaction()
    }

    private fun logEmvTags() {
        //EMV_TAG_TM_CAP
        logEmvTag(TAG, "KeyIdx", EMVTag.EMV_TAG_TM_CAPKINDEX)
        logEmvTag(TAG, "TVR", EMVTag.EMV_TAG_TM_TVR)
        logEmvTag(TAG, "CVMR", EMVTag.EMV_TAG_TM_CVMRESULT)
        logEmvTag(TAG, "AIP", EMVTag.EMV_TAG_IC_AIP)
        logEmvTag(TAG, "TC", EMVTag.EMV_TAG_TM_CAP)
        logEmvTag(TAG, "ADTC", EMVTag.EMV_TAG_TM_CAP_AD)
        logEmvTag(TAG, "TrxCurrCode", EMVTag.EMV_TAG_TM_CURCODE)
        logEmvTag(TAG, "TermCCode", EMVTag.EMV_TAG_TM_CNTRYCODE)
        logEmvTag(TAG, "TranType", EMVTag.EMV_TAG_TM_TRANSTYPE)
        logEmvTag(TAG, "Amt", EMVTag.EMV_TAG_TM_AUTHAMNTN)
        logEmvTag(TAG, "transLimit", EMVTag.M_TAG_TM_TRANS_LIMIT)
        logEmvTag(TAG, "transCdvmLimit", EMVTag.M_TAG_TM_TRANS_LIMIT_CDV)
        logEmvTag(TAG, "transCvmLimit", EMVTag.M_TAG_TM_CVM_LIMIT)
        logEmvTag(TAG, "transFloorLimit", EMVTag.M_TAG_TM_FLOOR_LIMIT)
        logEmvTag(TAG, "CurrExpo", EMVTag.EMV_TAG_TM_CUREXP)
        logEmvTag(TAG, "MerchID", EMVTag.EMV_TAG_TM_MCHID)
        logEmvTag(TAG, "MCC", EMVTag.EMV_TAG_TM_MCHCATCODE)
        logEmvTag(TAG, "TermID", EMVTag.EMV_TAG_TM_TERMID)
    }

    private suspend fun rebootTransaction(){
        Log.d(TAG, "StepGoOnChip rebootTransaction")
        emvSteps.stopTransaction()
        securePayment.endTransaction()
    }
}