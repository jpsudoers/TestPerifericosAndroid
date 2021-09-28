package com.ingenico.acc

import android.app.Application
import android.util.Log
import com.ingenico.acc.workflow.ResultCode.Companion.SPA_OK
import com.ingenico.ingp.dev.security.*
import com.ingenico.ingp.emv.*
import com.ingenico.ingp.emv.PinEntryStatus
import com.ingenico.ingp.p2pe.EncryptionAlgorithm
import com.ingenico.ingp.p2pe.EncryptionModel
import com.ingenico.ingp.p2pe.SdeConfig
import com.ingenico.ingp.p2pe.WhitelistMaskingConfig
import com.ingenico.ingp.secure.client.lib.ErrorCode
import com.ingenico.ingp.secure.client.lib.RemoteSdeProvider
import com.ingenico.ingp.secure.client.lib.emv.SdeEmvConfigHolder
import com.ingenico.ingp.secure.client.lib.reader.IccReaderResult
import com.ingenico.ingp.types.emv.PinVerifyResult
import com.ingenico.ingp.types.emv.SelectionMethod
import com.ingenico.ingp.types.emv.Track2EquivalentData
import com.ingenico.ingp.types.hexToByteArray
import com.ingenico.ingp.types.iso7816.Aid
import com.ingenico.ingp.types.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.lang.Long.parseLong

/**
 * A singleton interface to SPA API to ease state sharing between the Activities/Fragments of the
 * project.
 */

enum class EnumKeyAlgorithm
{
    TDES,
    DUKPT
}

enum class EnumKeyType {
    DATA,
    MAC,
    PIN
}


enum class EnumPinKeyEvent {
    NUMERIC_KEY,
    SHOW_KEYBOARD,
    KEY_CONFIRM,
    KEY_CANCEL,
    KEY_CLEAR,
    KEY_TIMEOUT,

    OFFLINE_PIN_LAST_TRY,
    OFFLINE_PIN_HAS_BEEN_BLOCKED,
    OFFLINE_PIN_WRONG,
}



interface ISecurePayment{
    fun onPinKeyboardEvent(pinEvent: EnumPinKeyEvent, pinResult: PinResult)
    interface PinResult {fun setResult(result: Int)}
}

interface ServiceConnection {
    fun onConnected(status: ErrorCode)
}

object SecurePayment{

    private val TAG = SecurePayment::class.java.simpleName
    private lateinit var remoteSdeProvider: RemoteSdeProvider
    private var iSecurePayment : ISecurePayment? = null
    var firstCvm  = CvmAction.NO_CVM
    private var resetTriesOffline = false
    private var trysLeft = 3

    private val channelPin = Channel<Int>()
    private val outputPinResult = object : ISecurePayment.PinResult {
        override fun setResult(result: Int) {
            CoroutineScope(Dispatchers.Default).launch {
                channelPin.send(result)
            }
        }
    }

    fun initialize(application: Application) {
        remoteSdeProvider = RemoteSdeProvider(application)
    }

    fun setInterface(iSecurePayment: ISecurePayment){
        this.iSecurePayment = iSecurePayment
    }

    fun connect(serviceConnection: ServiceConnection?) {
        var result: ErrorCode = ErrorCode.ERROR_SERVICE_NOT_AVAILABLE

        val job = CoroutineScope(Dispatchers.Default).launch {
            result = remoteSdeProvider.connect()

            if (result == ErrorCode.OK) {
                Log.i(TAG, "Connected to Secure Payment Service.")
            } else {
                Log.e(TAG, "ERROR: Connect to Secure Payment Service is failed.")
            }
        }

        job.invokeOnCompletion {
            serviceConnection?.onConnected(result)
        }
    }

    fun disconnect() {
        remoteSdeProvider.disconnect()
    }

    fun startTransaction() {
        val emvConfigValue = buildEmvConfig {
            selectionMethod(SelectionMethod.AID_ONLY)
            flagIccLog(true)
        }

        val sdeEmvConfigHolder = SdeEmvConfigHolder(emvConfigValue)

        remoteSdeProvider.startTransaction(
            sdeEmvConfigHolder,
            SdeConfig(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9").map {
                    WhitelistMaskingConfig(whitelistBin = it)
                },
                EncryptionModel.MODEL_2017,
                EncryptionAlgorithm.ALGO_TDES_16,
                BlockCipher.AES
            )
        )
    }

    suspend fun startTransaction(emvConfigValue: EmvConfigValue, supportSwipe: Boolean,supportChip: Boolean,supportCless: Boolean,timeoutSearchCardS: Int) : DetectionResult {

        val sdeEmvConfigHolder = SdeEmvConfigHolder(emvConfigValue)

        Log.d(TAG, "remoteSdeProvider startTransaction")

        remoteSdeProvider.startTransaction(
            sdeEmvConfigHolder,
            SdeConfig(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9").map {
                    WhitelistMaskingConfig(whitelistBin = it)
                },
                EncryptionModel.MODEL_2017,
                EncryptionAlgorithm.ALGO_TDES_16,
                BlockCipher.AES
            )
        )

        var detectionResult: DetectionResult
        var communicationErrorCount = -1

        do {
            communicationErrorCount++

            Log.d(TAG, "remoteSdeProvider seatchCard")

            detectionResult = remoteSdeProvider.getCardDetection().searchCard(
                iccInterface = supportChip,
                clessInteface = supportCless,
                swipeInterface = supportSwipe,
                timeoutMs = timeoutSearchCardS * 1000
            )

            if( detectionResult.cardDetectedOn == DetectionInterface.CLESS_READER ||detectionResult.cardDetectedOn == DetectionInterface.ICC_READER )
                resetTriesOffline = true

        } while ((detectionResult.errorMessage == "Communication error" ||
                    detectionResult.errorMessage == "Card timeout")  && communicationErrorCount < 30)

        Log.d(TAG, "Communication error count = $communicationErrorCount")

        return detectionResult
    }

    fun endTransaction(){
        try {
            Log.d(TAG, "endTransaction - stopSearch")
            getCard().stopSearch()
        }
        catch (ex : Exception){
            Log.d(TAG, ex.message ?: "")
        }
        finally {
            Log.d(TAG, "endTransaction - remoteSdeProvider endTransaction")
            remoteSdeProvider.endTransaction()
        }
    }

    fun getCard() : CardDetection {
        return remoteSdeProvider.getCardDetection()
    }

    fun getIccReader() : IccReaderResult? {
        return remoteSdeProvider.getIccReader()
    }

    fun getEmvSteps() : EmvSteps {
        return remoteSdeProvider.getEmvSteps()
    }

    fun getTrack2EMV() : String? {
        return remoteSdeProvider.getEmvSteps().getTrack2EquivalentData()?.toStandardFormatString()
    }

    fun loadSessionKey(mkKeyId: String, sessionkeyId: String, keyType: EnumKeyType, sessionKeyCiphered: String)
    {
        val secureElement = remoteSdeProvider.getSecureElement()
        val terminalMasterKey = secureElement.getKeyEncryptionKey(mkKeyId)

        when(keyType)
        {
            EnumKeyType.DATA -> {
                val result = terminalMasterKey.loadDataKey(sessionkeyId, sessionKeyCiphered.hexToByteArray())
                if (result == KeyStatus.OK) {
                    Log.i(TAG, "Load Data key successful")
                } else {
                    Log.i(TAG, "Saving Data key error")
                }
            }
            EnumKeyType.MAC -> {
                val result = terminalMasterKey.loadMacKey(sessionkeyId, sessionKeyCiphered.hexToByteArray())
                if (result == KeyStatus.OK) {
                    Log.i(TAG, "Load MAC key successful")
                } else {
                    Log.i(TAG, "Saving MAC key error")
                }
            }
            EnumKeyType.PIN -> {
                val result = terminalMasterKey.loadPinKey(sessionkeyId, sessionKeyCiphered.hexToByteArray())
                if (result == KeyStatus.OK) {
                    Log.i(TAG, "Load PIN key successful")
                } else {
                    Log.i(TAG, "Saving PIN key error")
                }
            }

        }
    }



    private suspend fun callPinListener(event: EnumPinKeyEvent): Int {

        iSecurePayment?.onPinKeyboardEvent(event, outputPinResult)

        return if(event == EnumPinKeyEvent.OFFLINE_PIN_LAST_TRY
            || event == EnumPinKeyEvent.OFFLINE_PIN_HAS_BEEN_BLOCKED
            || event == EnumPinKeyEvent.OFFLINE_PIN_WRONG)
            channelPin.receive()
        else
            SPA_OK
    }

    suspend fun startPinEntryOnline(keyId: String, algorithm: EnumKeyAlgorithm, pan: String, timeoutSec : Int) : PinEntryResult
    {
        var pinEntryResult : PinEntryResult
        val secureElement = remoteSdeProvider.getSecureElement()

        Log.i(TAG, "startPinEntryOnline keyId = [$keyId] algorithm = [$algorithm] " +
                "pan = [$pan] timeoutSec = [$timeoutSec]")

        val pinKey: PinKey = if(algorithm==EnumKeyAlgorithm.DUKPT) {
            val keySet = secureElement.getDukptKeys(keyId)
            keySet.pinKey
        } else {
            secureElement.getPinKey(keyId)
        }

        callPinListener(EnumPinKeyEvent.SHOW_KEYBOARD)

        Log.i(TAG, "startPinEntry")

        pinEntryResult = pinKey.startPinEntry(
            format = PinBlockFormat.BLOCK_FORMAT_0,
            pan = pan,
            returnOnFirstKey = true,
            maxPinLength = 12,
            minPinLength = 4,
            firstKeyEntryTimeout = timeoutSec,
            nextKeyEntryTimeout = timeoutSec,
            allowBypass = false
        )

        Log.i(TAG, "startPinEntry result $pinEntryResult")

        if(pinEntryResult.status == com.ingenico.ingp.dev.security.PinEntryStatus.TIMEOUT){
            callPinListener(EnumPinKeyEvent.KEY_TIMEOUT)
            return pinEntryResult
        }

        when (pinEntryResult.keyType) {
            42 -> callPinListener(EnumPinKeyEvent.NUMERIC_KEY)
            27 -> callPinListener(EnumPinKeyEvent.KEY_CANCEL)
            101 -> callPinListener(EnumPinKeyEvent.KEY_CLEAR)
        }

        while (pinEntryResult.status == com.ingenico.ingp.dev.security.PinEntryStatus.NEXT) {
            Log.i(TAG, "continuePinEntry")

            pinEntryResult = pinKey.continuePinEntry()

            Log.i(TAG, "continuePinEntry result $pinEntryResult")

            if(pinEntryResult.status == com.ingenico.ingp.dev.security.PinEntryStatus.TIMEOUT){
                callPinListener(EnumPinKeyEvent.KEY_TIMEOUT)
                return pinEntryResult
            }

            when (pinEntryResult.keyType) {
                42 -> callPinListener(EnumPinKeyEvent.NUMERIC_KEY)
                27 -> callPinListener(EnumPinKeyEvent.KEY_CANCEL)
                101 -> callPinListener(EnumPinKeyEvent.KEY_CLEAR)
            }
        }

        if(pinEntryResult.status == com.ingenico.ingp.dev.security.PinEntryStatus.PIN_AVAILABLE)
            callPinListener(EnumPinKeyEvent.KEY_CONFIRM)

        return pinEntryResult

    }

    suspend fun startPinEntryOffline() : CvmResult? {
        val secureElement = remoteSdeProvider.getSecureElement()
        val emvSteps = remoteSdeProvider.getEmvSteps()
        val pinVerifyResult: PinVerifyResult?
        val pinEntryStatus: PinEntryStatus
        var cvmResult: CvmResult
        val offlinePinEntry = secureElement.getOfflinePinEntry()

        if(resetTriesOffline){
            val pinCounter = emvSteps.getTagFromKernel(EMVTag.EMV_TAG_IC_PINTRYCNTR.toLong(radix = 16))?.toHexString()
            if(pinCounter != null)
                trysLeft = parseLong(pinCounter, 16).toInt()
            resetTriesOffline=false
        }

        Log.i(TAG, "startPinEntryOffline resetTriesOffline = $trysLeft")

        if(trysLeft==1)
            callPinListener(EnumPinKeyEvent.OFFLINE_PIN_LAST_TRY)

        callPinListener(EnumPinKeyEvent.SHOW_KEYBOARD)

        Log.i(TAG, "startPinEntry")

        var pinEntryResult = offlinePinEntry.startPinEntry(
            minPinLength = 4,
            maxPinLength = 12,
            allowBypass = false,
            returnOnFirstKey = true,
            firstKeyEntryTimeout = 30,
            nextKeyEntryTimeout = 30)

        Log.i(TAG, "startPinEntry result $pinEntryResult")

        when (pinEntryResult.keyType) {
            42 -> callPinListener(EnumPinKeyEvent.NUMERIC_KEY)
            27 -> callPinListener(EnumPinKeyEvent.KEY_CANCEL)
            101 -> callPinListener(EnumPinKeyEvent.KEY_CLEAR)
        }

        while (pinEntryResult.status == com.ingenico.ingp.dev.security.PinEntryStatus.NEXT) {

            Log.i(TAG, "continuePinEntry")

            pinEntryResult =offlinePinEntry.continuePinEntry()

            Log.i(TAG, "continuePinEntry result $pinEntryResult")

            when (pinEntryResult.keyType) {
                42 -> callPinListener(EnumPinKeyEvent.NUMERIC_KEY)
                27 -> callPinListener(EnumPinKeyEvent.KEY_CANCEL)
                101 -> callPinListener(EnumPinKeyEvent.KEY_CLEAR)
            }
        }

        if(pinEntryResult.status == com.ingenico.ingp.dev.security.PinEntryStatus.PIN_AVAILABLE)
            callPinListener(EnumPinKeyEvent.KEY_CONFIRM)

        pinEntryStatus = pinEntryResult.status.toEmvPinEntryStatus()

        Log.i(TAG, "pinEntryStatus = $pinEntryStatus")

        if(pinEntryStatus == PinEntryStatus.ENTRY_CANCEL || pinEntryStatus == PinEntryStatus.ENTRY_TIMEOUT) {
            if(pinEntryStatus == PinEntryStatus.ENTRY_TIMEOUT)
                callPinListener(EnumPinKeyEvent.KEY_TIMEOUT)

            return null
        }
        else
            trysLeft -= 1

        do {
            cvmResult =emvSteps.cardholderVerification(pinEntryStatus = pinEntryStatus)
            Log.i(TAG, "cardholderVerification begin = $cvmResult")

        } while (cvmResult.nextCvm == CvmAction.OFFLINE_PIN)

        val FMT_PLAIN_TEXT: Byte = 0
        val FMT_ENCRYPTED_TEXT: Byte = 1

        if (cvmResult.nextCvm == CvmAction.VERIFY_OFFLINE_PIN) {
            val pinFormat : Byte = if (cvmResult.isEncryptedPinRequired)
                FMT_ENCRYPTED_TEXT
            else
                FMT_PLAIN_TEXT

            val verifyResult =
                offlinePinEntry.verifyIccData(
                    pinFormat,
                    0,
                    0.toByte(),
                    cvmResult.random,
                    cvmResult.capk
                ) as? ByteArray

            Log.i(TAG, "verifyIccData = ${verifyResult?.toHexString() ?: ""}")

            pinVerifyResult = verifyResult?.let {
                if (it.size >= 3) {
                    PinVerifyResult(
                        sw1 = it[0],
                        sw2 = it[1],
                        apduRet = it[2]
                    )
                } else {
                    return cvmResult
                }
            }
            if (pinVerifyResult?.isVerifyOfflinePinSuccess() == true)
                Log.i(TAG, "Verify offline pin successfully.")
            else if(trysLeft==0 && pinEntryStatus != PinEntryStatus.ENTRY_CANCEL)
                callPinListener(EnumPinKeyEvent.OFFLINE_PIN_HAS_BEEN_BLOCKED)
            else
                callPinListener(EnumPinKeyEvent.OFFLINE_PIN_WRONG)

            cvmResult = emvSteps.cardholderVerification(pinEntryStatus = pinEntryStatus,offlinePinVerifyResult = pinVerifyResult)
            Log.i(TAG, "cardholderVerification final = $cvmResult")
        }

        return cvmResult
    }

    fun encryptData(keyId: String, algorithm: EnumKeyAlgorithm, inputData: ByteArray) : ByteArray?
    {
        val secureElement = remoteSdeProvider.getSecureElement()

        return when(algorithm) {
            EnumKeyAlgorithm.DUKPT -> {
                val keySet = secureElement.getDukptKeys(keyId)
                val dataEncryptionKey = keySet.dataKey
                dataEncryptionKey.encrypt(inputData)
            }
            EnumKeyAlgorithm.TDES -> {
                val dataEncryptionKey = secureElement.getDataEncryptionKey(keyId)
                dataEncryptionKey.encrypt(inputData)
            }
        }
    }


    fun decryptData(keyId: String, algorithm: EnumKeyAlgorithm, inputData: ByteArray) : ByteArray?
    {
        val secureElement = remoteSdeProvider.getSecureElement()
        var outputData : ByteArray? = null
        when(algorithm) {
            EnumKeyAlgorithm.DUKPT -> {
                val keySet = secureElement.getDukptKeys(keyId)
                val dataEncryptionKey = keySet.dataKey
                outputData = dataEncryptionKey.decrypt(inputData)
            }
            EnumKeyAlgorithm.TDES -> {
                val dataEncryptionKey = secureElement.getDataEncryptionKey(keyId)
                outputData = dataEncryptionKey.decrypt(inputData)
            }
        }
        return outputData
    }


    fun computeMac(keyId: String, algorithm: EnumKeyAlgorithm, inputData: ByteArray, macAlgorithm: MacAlgorithm) : ByteArray?
    {
        val secureElement = remoteSdeProvider.getSecureElement()

        return when(algorithm) {
            EnumKeyAlgorithm.DUKPT -> {
                val keySet = secureElement.getDukptKeys(keyId)
                val dataEncryptionKey = keySet.macKey
                dataEncryptionKey.computeMac(inputData, macAlgorithm)
            }
            EnumKeyAlgorithm.TDES -> {
                val dataEncryptionKey = secureElement.getDataEncryptionKey(keyId)
                dataEncryptionKey.decrypt(inputData)
            }
        }
    }


    fun getKSN(keyId: String): String
    {
        try{
            val secureElement = remoteSdeProvider.getSecureElement()
            val keySet = secureElement.getDukptKeys(keyId)
            return keySet.ksn.toHexString()
        }
        catch (ex : Exception){
            Log.d(TAG, "getKSN - Exception, No injected Key ${ex.message}")
        }

        return ""
    }

    fun incrementKSN(keyId: String): String
    {
        try {
            val secureElement = remoteSdeProvider.getSecureElement()
            val keySet = secureElement.getDukptKeys(keyId)
            secureElement.increaseKsn(keySet.id)
            return keySet.ksn.toHexString()
        }
        catch (ex : Exception){
            Log.d(TAG, "incrementKSN - Exception, No injected Key ${ex.message}")
        }

        return ""
    }

    private fun Track2EquivalentData.toStandardFormatString(): String
    {
        return "${pan.value}=$expirationDate$serviceCode$discretionaryData"
    }

    fun getSecureElement() : SecureElement {
        return remoteSdeProvider.getSecureElement()
    }

    private fun PinVerifyResult.isVerifyOfflinePinSuccess(): Boolean {
        return this.apduRet == 0x00.toByte() &&
                this.sw2 == 0x00.toByte() &&
                this.sw1 == 0x90.toByte()
    }

    fun removePadding(s: ByteArray): String {
        return s.toHexString().replace("80+$".toRegex(), "")
    }

}
