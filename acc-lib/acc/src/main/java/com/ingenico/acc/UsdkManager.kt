package com.ingenico.acc

import android.app.Application
import android.util.Log
import com.ingenico.ingp.dev.sdk.ErrorCode
import com.ingenico.ingp.dev.sdk.UsdkDeviceService
import com.usdk.apiservice.aidl.beeper.UBeeper
import com.usdk.apiservice.aidl.device.DeviceInfo
import com.usdk.apiservice.aidl.led.ULed
import com.usdk.apiservice.aidl.printer.UPrinter
import kotlinx.coroutines.runBlocking

object UsdkManager {
    private val TAG = UsdkManager::class.java.simpleName
    lateinit var deviceService: UsdkDeviceService

    fun initialize(context: Application) {
        deviceService = UsdkDeviceService(context)
    }

    fun connect() {
        runBlocking {
            val result = deviceService.connect()

            if (result == ErrorCode.OK) {
                Log.i(TAG, "Connected to Usdk manager")
            } else {
                Log.e(TAG, "ERROR: Connect to Usdk Manager is failed.")
            }
        }
    }

    fun disconnect() {
        runBlocking {
            deviceService.release()
        }
    }

    fun getLed(): ULed? {
        return deviceService.getLED()
    }

    fun getBeeper(): UBeeper? {
        return deviceService.getBeeper()
    }

    fun getPrinter(): UPrinter? {
        return deviceService.getPrinter()
    }

    fun getDevice(): DeviceInfo {
        return deviceService.getDeviceManager()?.deviceInfo!!
    }
}
