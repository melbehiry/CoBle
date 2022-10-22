package com.elbehiry.coble.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import java.util.UUID

data class Scan(
    val device: BluetoothDevice,
    val services: Map<UUID, ByteArray?>
)

@SuppressLint("MissingPermission")
internal fun ScanResult.toScan(): Scan {
    return Scan(
        device = device,
        services = scanRecord?.serviceUuids
            .orEmpty()
            .associate { parcelUuid: ParcelUuid ->
                parcelUuid.uuid to scanRecord?.getServiceData(parcelUuid)
            }
    )
}
