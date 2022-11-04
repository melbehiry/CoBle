package com.elbehiry.coble.scanner

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.elbehiry.coble.bluetooth.BleDeviceMatcher
import com.elbehiry.coble.bluetooth.BluetoothError
import com.elbehiry.coble.bluetooth.toBluetoothError
import com.elbehiry.coble.extensions.requireBle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import java.util.regex.Pattern

internal fun BluetoothScanner.Companion.create(
    bluetoothAdapter: BluetoothAdapter
): BluetoothScanner {
    return AndroidBluetoothScanner(
        bluetoothAdapter = bluetoothAdapter
    )
}

interface BluetoothScanner {

    fun startScanning(
        scanningConfig: ScanningConfig = createScanningConfig {  }
    ): Flow<Result>

    fun stopScanning()

    sealed class Result {
        data class Success(val scan: Scan) : Result()
        data class Error(val error: BluetoothError) : Result()
    }

    companion object
}

private class AndroidBluetoothScanner(
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothScanner {

    private val seenBleDevices = hashSetOf<String>()
    private var callBack: ScanCallback? = null

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_SCAN])
    override fun startScanning(scanningConfig: ScanningConfig): Flow<BluetoothScanner.Result> =

        callbackFlow {

            val bluetoothLeScanner = requireBle(bluetoothAdapter.bluetoothLeScanner)

            callBack = object : ScanCallback() {

                @SuppressLint("MissingPermission")
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    if (!seenBleDevices.contains(result.device.address) &&
                        ifDeviceNameMatched(result, scanningConfig.namePatterns)
                    ) {
                        seenBleDevices.add(result.device.address)
                        trySend(BluetoothScanner.Result.Success(result.toScan()))
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    trySend(BluetoothScanner.Result.Error(errorCode.toBluetoothError()))
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    if (results.isEmpty()) return

                    results.filter {
                        !seenBleDevices.contains(it.device.address)
                    }.map { result ->
                        seenBleDevices.add(result.device.address)
                        result.toScan()
                    }.forEach {
                        trySend(BluetoothScanner.Result.Success(it))
                    }
                }
            }

            bluetoothLeScanner.startScan(
                scanningConfig.scanFilters,
                scanningConfig.scanSettings,
                callBack
            )

            awaitClose {
                bluetoothLeScanner.stopScan(callBack)
                seenBleDevices.clear()
            }
        }.onStart {
            seenBleDevices.clear()
        }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_SCAN])
    override fun stopScanning() {
        if (callBack != null) {
            val bluetoothLeScanner = requireBle(bluetoothAdapter.bluetoothLeScanner)
            bluetoothLeScanner.stopScan(callBack)
        }
    }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    private fun ifDeviceNameMatched(result: ScanResult, namePatterns: List<Pattern>): Boolean {
        return if (namePatterns.isEmpty()) {
            true
        } else {

            var matched = false

            namePatterns.forEach check@{
                if (BleDeviceMatcher.matchesName(it, result.device)) {
                    matched = true
                    return@check
                }
            }
            return matched
        }
    }
}