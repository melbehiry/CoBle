package com.elbehiry.coble.scanner

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import com.elbehiry.coble.BluetoothIdentifiers

interface ScanningConfig {
    val filters: List<ScanFilter>?
    val scanSettings: ScanSettings
}

class DefaultScanningConfig constructor(
    bluetoothIdentifiers: BluetoothIdentifiers
) : ScanningConfig {

    override val filters: List<ScanFilter> = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(bluetoothIdentifiers.service))
            .build()
    )
    override val scanSettings: ScanSettings = ScanSettings.Builder().build()
}