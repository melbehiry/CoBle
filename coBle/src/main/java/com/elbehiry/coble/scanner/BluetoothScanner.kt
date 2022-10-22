package com.elbehiry.coble.scanner

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_SCAN
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.elbehiry.coble.BluetoothError
import com.elbehiry.coble.requireBle
import com.elbehiry.coble.toBluetoothError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal fun BluetoothScanner.Companion.create(
    bluetoothAdapter: BluetoothAdapter
): BluetoothScanner {
    return AndroidBluetoothScanner(
        bluetoothAdapter = bluetoothAdapter
    )
}

interface BluetoothScanner {

    fun startScanning(scanningConfig: ScanningConfig): Flow<Result>

    sealed class Result {
        data class Success(val scans: List<Scan>) : Result()
        data class Error(val error: BluetoothError) : Result()
    }

    companion object
}

private class AndroidBluetoothScanner(
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothScanner {

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_SCAN])
    override fun startScanning(scanningConfig: ScanningConfig): Flow<BluetoothScanner.Result> =
        callbackFlow {

            val bluetoothLeScanner = requireBle(bluetoothAdapter.bluetoothLeScanner)

            val callBack = object : ScanCallback() {

                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val scans = listOf(result.toScan())
                    trySend(BluetoothScanner.Result.Success(scans))
                }

                override fun onScanFailed(errorCode: Int) {
                    trySend(BluetoothScanner.Result.Error(errorCode.toBluetoothError()))
                }

                override fun onBatchScanResults(results: MutableList<ScanResult>) {
                    if (results.isEmpty()) return

                    val scans = results.map { result -> result.toScan() }
                    trySend(BluetoothScanner.Result.Success(scans))
                }
            }

            bluetoothLeScanner.startScan(
                scanningConfig.filters, scanningConfig.scanSettings, callBack
            )

            awaitClose { bluetoothLeScanner.stopScan(callBack) }
        }
}