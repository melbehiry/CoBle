package com.elbehiry.coble.scanner

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.elbehiry.coble.BluetoothIdentifiers

fun ScanningConfig.Companion.create(
    bluetoothIdentifiers: BluetoothIdentifiers
): ScanningConfig = DefaultScanningConfig(
    bluetoothIdentifiers = bluetoothIdentifiers
)

interface ScanningConfig {
    val filters: List<ScanFilter>?
    val scanSettings: ScanSettings

    companion object
}

private class DefaultScanningConfig(
    bluetoothIdentifiers: BluetoothIdentifiers
) : ScanningConfig {

    private val filtersList = mutableListOf<ScanFilter>()

    init {
        bluetoothIdentifiers.serviceUUID?.let { uuid->
            filtersList.add(
                ScanFilter.Builder().setServiceUuid(ParcelUuid(uuid)).build()
            )
        }

        bluetoothIdentifiers.deviceAddress?.let {  address->
            filtersList.add(
                ScanFilter.Builder().setDeviceAddress(address).build()
            )
        }

        bluetoothIdentifiers.deviceName?.let { deviceName ->
            filtersList.add(
                ScanFilter.Builder().setDeviceName(deviceName).build()
            )
        }
    }

    override val filters: List<ScanFilter> = filtersList

    override val scanSettings: ScanSettings = ScanSettings.Builder().build()
}