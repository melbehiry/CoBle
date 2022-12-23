package com.elbehiry.coble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.elbehiry.coble.connector.BluetoothConnector
import com.elbehiry.coble.connector.create
import com.elbehiry.coble.features.FeatureConf
import com.elbehiry.coble.scanner.BluetoothScanner
import com.elbehiry.coble.scanner.OneShotBluetoothScanner
import com.elbehiry.coble.scanner.ScanningTimeout
import com.elbehiry.coble.scanner.create

internal val LoggingTag = CoBle.javaClass.simpleName

object CoBle {
    lateinit var scanner: BluetoothScanner
    lateinit var connector: BluetoothConnector
    lateinit var oneShotScanner: OneShotBluetoothScanner

    private val allFeatures = mutableMapOf<String, FeatureConf>()
    private var initiated = false

    fun init(config: BleConfiguration) {
        allFeatures.putAll(config.features)
        initSDK(config.context, config.scanningTimeOutInSeconds)
    }

    private fun initSDK(context: Context, scanningTimeOutInSeconds: Int) {
        if (initiated) {
            return
        }

        synchronized(this) {
            if (initiated) {
                return
            }

            val bluetoothManager: BluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
            val scanningTimeOut = ScanningTimeout.create(scanningTimeOutInSeconds)

            scanner = BluetoothScanner.create(bluetoothAdapter)
            oneShotScanner = OneShotBluetoothScanner.create(scanner, scanningTimeOut)
            connector = BluetoothConnector.create(context = context)
            initiated = true

            allFeatures.forEach { (_, featureConfig) ->
                featureConfig.create(context)
            }
        }
    }

    suspend fun release() {
        if (!initiated) {
            return
        }

        connector.release()
        allFeatures.forEach { (_, featureConfig) ->
            featureConfig.stop()
        }
    }
}