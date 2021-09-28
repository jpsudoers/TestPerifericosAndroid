package com.ingenico.acc.workflow

import android.util.Log
import com.ingenico.acc.SecurePayment
import com.ingenico.acc.UsdkManager.getLed
import com.ingenico.acc.logEmvTag
import com.ingenico.acc.workflow.ResultCode.Companion.SPA_CANCEL
import com.ingenico.acc.workflow.ResultCode.Companion.SPA_RETRY_GET_CARD
import com.ingenico.acc.workflow.model.GetCardModel
import com.ingenico.ingp.emv.*
import com.ingenico.ingp.types.ber.TagValue
import com.ingenico.ingp.types.emv.encoded
import com.ingenico.ingp.types.iso7813.RawMagStripe
import com.ingenico.ingp.types.iso7816.Aid
import com.ingenico.ingp.types.toHexString
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

interface IStepGetCard{
    fun onSetSpecificEmvParams(aid: String, kernelId: KernelId) : List<TagValue>

    fun onShowMenuAppSelection(appArrayList: Array<String?>, menuResult: MenuResult)

    interface MenuResult {fun setResult(result: Int)}

    fun onShowProcessing()

    fun onEmvFlow(result: GetCardEmvErrors, emvFlowResult: EmvFlowResult)

    interface EmvFlowResult {fun setResult(result: FlowResult)}

    fun onOutputResult(result: Int, getCardResult: GetCardResult?)
}

class StepGetCard(private val securePayment: SecurePayment, private val iStepGetCard: IStepGetCard) {

    private val channelMenu = Channel<Int>()
    private val channelEmvFlow = Channel<FlowResult>()
    private lateinit var emvSteps : EmvSteps
    private var job : Job? = null

    companion object {
        private val TAG = StepGetCard::class.java.simpleName
    }

    private val outputMenuResult = object : IStepGetCard.MenuResult {
        override fun setResult(result: Int) {
            CoroutineScope(Dispatchers.Default).launch {
                channelMenu.send(result)
            }
        }
    }

    private val outputEmvFlowResult = object : IStepGetCard.EmvFlowResult {
        override fun setResult(result: FlowResult) {
            CoroutineScope(Dispatchers.Default).launch {
                channelEmvFlow.send(result)
            }
        }
    }

    fun execute(emvConfigValue: EmvConfigValue, getCardModel: GetCardModel){

        job = CoroutineScope(Dispatchers.Default).launch {
            try {
                Log.d(TAG, "execute: $getCardModel")

                if(getCardModel.supportCless)
                    getLed()?.turnOn(1)

                //Start SPA transaction and Wait for card entry event
                val detectionResult = securePayment.startTransaction(emvConfigValue,
                    getCardModel.supportSwipe,
                    getCardModel.supportChip,
                    getCardModel.supportCless,
                    getCardModel.timeoutSearchCard)

                Log.d(TAG, "=====================================================")
                Log.d(TAG, "detectionResult: $detectionResult")
                Log.d(TAG, "is job active? ${job?.isActive}")

                if(job?.isActive == false)
                    return@launch

                if(detectionResult.cardDetectedOn == DetectionInterface.CLESS_READER &&
                    (detectionResult.clessCardType == ClessCardType.S50
                            || detectionResult.clessCardType == ClessCardType.S70)){
                    emvGeneralError()
                    return@launch
                }

                //Check errors of card entry
                if(detectionResult.error != null){
                    when (detectionResult.error) {
                        DetectionError.DOUBLE_CARD_IN_RANGE -> {
                            iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_DOUBLE_CARD_IN_RANGE, outputEmvFlowResult)
                        }
                        DetectionError.SWIPE_FAIL -> {
                            Log.d(TAG, "detectionResult err! -> swipe fail")
                            iStepGetCard.onOutputResult(SPA_CANCEL, null)
                        }
                        DetectionError.INSERT_FAIL -> {
                            Log.d(TAG, "detectionResult err! -> insert fail")
                            iStepGetCard.onOutputResult(SPA_CANCEL, null)
                        }
                        DetectionError.CLESS_FAIL -> {
                            Log.d(TAG, "detectionResult err! -> cless fail")
                            iStepGetCard.onOutputResult(SPA_CANCEL, null)
                        }
                        DetectionError.CANCELLED -> {
                            Log.d(TAG, "detectionResult err! -> Canceled")
                            iStepGetCard.onOutputResult(SPA_CANCEL, null)
                            return@launch
                        }
                        else -> {
                            Log.d(TAG, "detectionResult err! -> unknown error")
                            iStepGetCard.onOutputResult(SPA_CANCEL, null)
                            return@launch
                        }
                    }

                    //Wait for callback result...
                    if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                        iStepGetCard.onOutputResult(SPA_CANCEL, null)
                    else {
                        iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
                    }

                    rebootTransaction()
                    return@launch
                }

                //Check entry mode detected
                when (detectionResult.cardDetectedOn) {
                    DetectionInterface.MAG_READER -> {
                        iStepGetCard.onShowProcessing()
                        doSwipeTransaction(detectionResult.magStripeData)
                    }
                    DetectionInterface.CLESS_READER,
                    DetectionInterface.ICC_READER -> {
                        iStepGetCard.onShowProcessing()
                        doEmvTransaction(securePayment, getCardModel, detectionResult.cardDetectedOn!!)
                    }
                    else -> {
                        iStepGetCard.onOutputResult(ResultCode.SPA_TIMEOUT, null)
                        securePayment.endTransaction()
                    }
                }
            }
            catch (ex : Exception){
                Log.d(TAG, "Exception : Line[${ex.stackTrace[0].lineNumber}] message: $ex")
                emvGeneralError()
            }
        }
    }

    fun stop(){
        //Stop search card, end transaction and cancel coroutine
        Log.d(TAG, "StepGetCard stop - job Cancel request")
        job?.cancel()
        Log.d(TAG, "StepGetCard stop - turnOffLeds")
        turnOffLeds()
        Log.d(TAG, "StepGetCard stop - securePayment endTransaction")
        securePayment.endTransaction()
    }

    private fun doSwipeTransaction(magStripeData: RawMagStripe?) {
        turnOffLeds()

        val track1 = if(magStripeData?.track1?.data == null
            && magStripeData?.rawTrack1 != null){
            if(magStripeData.rawTrack1!!.length > 6)
                magStripeData.rawTrack1?.substring(2, magStripeData.rawTrack1!!.length - 2)!!
            else
                ""
        }else
            magStripeData?.track1?.data ?: ""

        val getCardResult = GetCardResult(
                entryMode = Transaction.MODE_MAG,
                track1 = track1,
                track2 = magStripeData?.track2?.data ?: "",
                track3 = magStripeData?.track3?.data ?: "",
                pan = magStripeData?.track2?.pan ?: "",
                last4pan = (magStripeData?.track2?.pan ?: "").takeLast(4),
                expirationDate = magStripeData?.track2?.expirationDate ?: "",
                serviceCode = magStripeData?.track2?.serviceCode ?: "",
                cardholderName = magStripeData?.track1?.name?.trim() ?: ""
        )

        //Get swipe card information
        Log.d(TAG, "getCardResult = $getCardResult")
        iStepGetCard.onOutputResult(ResultCode.SPA_OK, getCardResult)
    }

    private suspend fun doEmvTransaction(
        securePayment: SecurePayment,
        getCardModel: GetCardModel,
        cardDetectedOn: DetectionInterface,
        remainCardApplicationList: List<CardApplication> = emptyList()
    ) {
        // Get Emv steps & start app selection process
        emvSteps = securePayment.getEmvSteps()

        var cardApplicationList: MutableList<CardApplication> = remainCardApplicationList.toMutableList()

        if (remainCardApplicationList.isEmpty()) {
            val startSelectionResult = emvSteps.startSelection()
            Log.d(TAG, "EmvStep startSelection result = $startSelectionResult")

            if(processStepResult(startSelectionResult.error) == FlowResult.EMV_CANCEL)
                return

            cardApplicationList = startSelectionResult.candidateAidList?.toMutableList()!!
        }

        if(cardApplicationList.isEmpty()){
            iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_NO_APP, outputEmvFlowResult)
            //Wait for result...
            channelEmvFlow.receive()

            iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_REMOVE_CARD, outputEmvFlowResult)
            //Wait for result...
            channelEmvFlow.receive()

            iStepGetCard.onOutputResult(SPA_CANCEL, null)
            rebootTransaction()
            return
        }

        //Check priority indicator for contact-less cards with multi-application
        if (cardDetectedOn == DetectionInterface.CLESS_READER) {
            val app = cardApplicationList.minByOrNull {
                it.priorityIndicator?.toLong() ?: 0
            }
            if(app != null){
                cardApplicationList.clear()
                cardApplicationList.add(app)
            }
        }

        if(cardDetectedOn == DetectionInterface.CLESS_READER)
            getLed()?.turnOn(2)
        else
            turnOffLeds()

        // Get array list of app preferred names or labels
        val appArrayList = cardApplicationList.map {
            it.label
        }.toTypedArray()

        // Call callback to show App menu selection
        iStepGetCard.onShowMenuAppSelection(appArrayList, outputMenuResult)

        //Wait for menu selection result...
        val menuResult = channelMenu.receive()

        if(menuResult == SPA_CANCEL){
            iStepGetCard.onOutputResult(SPA_CANCEL, null)
            rebootTransaction()
            return
        }

        val finalSelectResult = emvSteps.finalSelect(cardApplicationList[menuResult].aid)
        Log.d(TAG, "EmvStep finalSelect result = $finalSelectResult")

        if(cardDetectedOn == DetectionInterface.CLESS_READER)
            getLed()?.turnOn(3)

        if(finalSelectResult.error == EmvError.EMV_RESULT_APPLOCK){
            iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_BLOCKED_APP, outputEmvFlowResult)

            //Wait for result...
            if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE) {
                iStepGetCard.onOutputResult(SPA_CANCEL, null)
                rebootTransaction()
                return
            }

            val remainingAidList = finalSelectResult.candidateAidList

            if (remainingAidList.isNullOrEmpty()) {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_NO_APP, outputEmvFlowResult)

                //Wait for result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE) {
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                    rebootTransaction()
                    return
                }

                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_REMOVE_CARD, outputEmvFlowResult)

                //Wait for result...
                channelEmvFlow.receive()
            }

            doEmvTransaction(securePayment, getCardModel, cardDetectedOn, remainingAidList
                ?: emptyList())

            return
        }
        else if(processStepResult(finalSelectResult.error) == FlowResult.EMV_CANCEL)
            return

        logEmvTag(TAG, "Card Holder Name", EMVTag.EMV_TAG_IC_CHNAME)
        logEmvTag(TAG, "term cap. before", EMVTag.EMV_TAG_TM_CAP)

        val specificParams = iStepGetCard.onSetSpecificEmvParams(finalSelectResult.selectedAid?.value?.toHexString()!!, finalSelectResult.kernelId!!)
        if(specificParams.isEmpty()){
            if(processStepResult(EmvError.EMV_RESULT_NOAPP) == FlowResult.EMV_CANCEL)
                return
        }

        val additionalData = buildTransactionData {
            serviceType(getCardModel.serviceType)
        }.toMutableList()

        additionalData.addAll(specificParams)

        val startTrx = emvSteps.startTransaction(
            getCardModel.amount,
            getCardModel.transactionType.encoded(),
            getCardModel.date,
            getCardModel.time,
            getCardModel.sequenceCounter,
            getCardModel.otherAmount,
            additionalData
        )
        Log.d(TAG, "EmvStep startTransaction result = $startTrx")

        if(processStepResult(startTrx.error) == FlowResult.EMV_CANCEL)
            return

        logEmvTag(TAG, "term cap. after", EMVTag.EMV_TAG_TM_CAP)

        if(cardDetectedOn == DetectionInterface.CLESS_READER)
            getLed()?.turnOn(4)

        val updateTrxResult =  emvSteps.updateTransactionData(
            amount = getCardModel.amount,
            otherAmount = getCardModel.otherAmount,
            additionalData = additionalData
        )
        Log.d(TAG, "EmvStep updateTransactionData result = $updateTrxResult")

        logEmvTag(TAG, "TVR", EMVTag.EMV_TAG_TM_TVR)

        if(processStepResult(updateTrxResult.error) == FlowResult.EMV_CANCEL)
            return

        if(updateTrxResult.firstCvm != null)
            SecurePayment.firstCvm = updateTrxResult.firstCvm!!
        else{
            emvGeneralError()
            return
        }

        if(SecurePayment.firstCvm == CvmAction.CONSUMER_DEVICE){

            turnOffLeds()

            iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_SEE_PHONE_INSTRUCTIONS, outputEmvFlowResult)
            channelEmvFlow.receive()

            iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_CLESS_TAP_CARD_AGAIN, outputEmvFlowResult)
            channelEmvFlow.receive()

            getLed()?.turnOn(1)

            val cvmResult: CvmResult?

            try {
                cvmResult = emvSteps.cardholderVerification(
                        cvmAccepted = true,
                        pinEntryStatus = PinEntryStatus.EXECUTE_CDCVM,
                        cdcvmDetectionTimeout = getCardModel.timeoutSearchCard * 1000
                )
            }catch (ex : IllegalStateException){
                //Exception generated when Coroutine is cancelled
                Log.d(TAG, "EmvStep cardholderVerification canceled")
                rebootTransaction()
                return
            }

            if(job?.isActive == false){
                rebootTransaction()
                return
            }

            getLed()?.turnOn(2)
            getLed()?.turnOn(3)
            getLed()?.turnOn(4)

            Log.d(TAG, "CONSUMER_DEVICE cvmResult = $cvmResult")

            if(cvmResult.nextCvm != null)
                SecurePayment.firstCvm = cvmResult.nextCvm!!
            else{
                emvGeneralError()
                return
            }
        }

        val entryMode = when(cardDetectedOn){
            DetectionInterface.ICC_READER -> Transaction.MODE_CHIP
            DetectionInterface.CLESS_READER -> Transaction.MODE_CTLS
            else -> Transaction.MODE_CTLS_MS
        }

        val label = if(emvSteps.getApplicationLabelName().isNullOrEmpty())
            cardApplicationList[menuResult].label
        else
            emvSteps.getApplicationLabelName()

        turnOffLeds()

        val getCardResult = GetCardResult(
                entryMode = entryMode,
                track1 = "",
                track2 = securePayment.getTrack2EMV().toString(),
                track3 = "",
                pan = emvSteps.getPan()?.value ?: "",
                last4pan = emvSteps.getPan()?.value?.takeLast(4) ?: "",
                expirationDate = emvSteps.getPanExpiryDate() ?: "",
                serviceCode = (emvSteps.getServiceCode() ?: "").takeLast(3),
                cardholderName = emvSteps.getCardholderName()?.trim() ?: "",
                PANSequenceNumber = emvSteps.getPanSequenceNumber()?.toInt() ?: 0,
                appLabel = label ?: "",
                firstCvm = SecurePayment.firstCvm)

        //Get Chip card information
        Log.d(TAG, "getCardResult = $getCardResult")
        iStepGetCard.onOutputResult(ResultCode.SPA_OK, getCardResult)
    }

    private suspend fun emvGeneralError() {
        iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_ACTIVATE_FAIL, outputEmvFlowResult)

        //Wait for callback result...
        if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
            iStepGetCard.onOutputResult(SPA_CANCEL, null)
        else {
            iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
        }

        rebootTransaction()
    }

    private suspend fun processStepResult(emvError: EmvError?) : FlowResult{

        if(emvError == null)
            return FlowResult.EMV_CONTINUE

        else when(emvError){

            null-> FlowResult.EMV_CONTINUE

            EmvError.EMV_RESULT_NOAPP -> {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_NO_APP, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else {
                    iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_REMOVE_CARD, outputEmvFlowResult)

                    //Wait for callback result...
                    if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                        iStepGetCard.onOutputResult(SPA_CANCEL, null)
                    else {
                        iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
                    }
                }
            }

            EmvError.POWERUP_FAIL -> {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_POWERUP_FAIL, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else {
                    iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_REMOVE_CARD, outputEmvFlowResult)

                    //Wait for callback result...
                    if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                        iStepGetCard.onOutputResult(SPA_CANCEL, null)
                    else {
                        iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_CHIP))
                    }
                }
            }

            EmvError.EMV_ERROR_SHOW_CARD_AGAIN -> {

                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_SHOW_CARD_AGAIN, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else {
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
                }
            }

            EmvError.EMV_RESULT_APDU_STATUS_ERROR -> {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_CARD_NOT_SUPPORTED, outputEmvFlowResult)

                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
            }

            EmvError.STATE_ERROR,
            EmvError.EMV_RESULT_APDU_ERROR -> {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_ACTIVATE_FAIL, outputEmvFlowResult)

                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
            }

            EmvError.SYSTEM_ERROR -> {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_SYSTEM_ERROR, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)

                SecurePayment.disconnect()
                SecurePayment.connect(null)

                FlowResult.EMV_CANCEL
            }

            EmvError.EMV_RESULT_EXCEED_CTLMT -> {

                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_EXCEED_CTLMT, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else {
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_CTLS))
                }
            }

            EmvError.EMV_RESULT_MISSING_TAGS -> {

                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_MISSING_TAG, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
            }

            EmvError.UNKNOWN_ERROR -> {
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_KERNEL_ERROR, outputEmvFlowResult)

                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
            }

            EmvError.UNEXPECTED_CDCVM_DETECTION ->{
                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_SEE_PHONE_INSTRUCTIONS, outputEmvFlowResult)

                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
            }

            else -> {

                iStepGetCard.onEmvFlow(GetCardEmvErrors.EMV_ACTIVATE_FAIL, outputEmvFlowResult)

                //Wait for callback result...
                if (channelEmvFlow.receive() != FlowResult.EMV_CONTINUE)
                    iStepGetCard.onOutputResult(SPA_CANCEL, null)
                else {
                    iStepGetCard.onOutputResult(SPA_RETRY_GET_CARD, GetCardResult(entryMode = Transaction.MODE_NONE))
                }
            }
        }

        rebootTransaction()
        return FlowResult.EMV_CANCEL
    }

    private fun turnOffLeds(){
        getLed()?.turnOff(1)
        getLed()?.turnOff(2)
        getLed()?.turnOff(3)
        getLed()?.turnOff(4)
    }

    private suspend fun rebootTransaction(){
        turnOffLeds()

        try {
            emvSteps.stopTransaction()
        }
        catch (ex: Exception){

        }
        finally {
            securePayment.endTransaction()
        }
    }
}
