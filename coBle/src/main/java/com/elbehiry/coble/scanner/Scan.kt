package com.elbehiry.coble.scanner

import android.annotation.SuppressLint
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import java.util.UUID

data class Scan(
    val address: String,
    val name: String?,
    val services: Map<UUID, ByteArray?>
)

@SuppressLint("MissingPermission")
internal fun ScanResult.toScan(): Scan {
    return Scan(
        address = device.address,
        name = device.name,
        services = scanRecord?.serviceUuids
            .orEmpty()
            .associate { parcelUuid: ParcelUuid ->
                parcelUuid.uuid to scanRecord?.getServiceData(parcelUuid)
            }
    )
}
