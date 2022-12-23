package com.elbehiry.coble.data

import android.bluetooth.BluetoothDevice
import com.elbehiry.coble.connector.BluetoothConnector
import com.elbehiry.coble.connector.ConnectionStateChanged
import com.elbehiry.coble.scanner.BluetoothScanner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface DevicesRepository {
    fun startScanning(): Flow<BluetoothScanner.Result>

    suspend fun connectDevice(bluetoothDevice: BluetoothDevice) : ConnectionStateChanged
}

class ScanningDevicesRepository @Inject constructor(
    private val scanner: BluetoothScanner,
    private val connector: BluetoothConnector
) : DevicesRepository {

    override fun startScanning(): Flow<BluetoothScanner.Result> = scanner.startScanning(
    )

    override suspend fun connectDevice(bluetoothDevice: BluetoothDevice) : ConnectionStateChanged =
        connector.connect(bluetoothDevice)
}
