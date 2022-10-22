package com.elbehiry.coble.scanner

import com.elbehiry.coble.ScanningTimedOut
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal fun OneShotBluetoothScanner.Companion.create(
    bluetoothScanner: BluetoothScanner,
    scanningTimeout: ScanningTimeout
): OneShotBluetoothScanner =
    FirstResultBluetoothScanner(
        bluetoothScanner = bluetoothScanner,
        scanningTimeout = scanningTimeout
    )

interface OneShotBluetoothScanner {
    suspend fun startScanning(scanningConfig: ScanningConfig): BluetoothScanner.Result

    companion object
}

private class FirstResultBluetoothScanner(
    private val bluetoothScanner: BluetoothScanner,
    private val scanningTimeout: ScanningTimeout
) : OneShotBluetoothScanner {
    override suspend fun startScanning(scanningConfig: ScanningConfig): BluetoothScanner.Result {
        return withTimeoutOrNull(scanningTimeout.timeout) {
            bluetoothScanner.startScanning(scanningConfig)
                .first { result ->
                    result is BluetoothScanner.Result.Error ||
                            result is BluetoothScanner.Result.Success && result.scans.isNotEmpty()
                }
        } ?: BluetoothScanner.Result.Error(ScanningTimedOut(scanningTimeout.timeout))
    }
}
