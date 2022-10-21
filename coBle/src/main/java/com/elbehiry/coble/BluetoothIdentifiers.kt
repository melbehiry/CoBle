package com.elbehiry.coble

import java.util.UUID

interface BluetoothIdentifiers {
    val service: UUID
    val message: UUID
    val serviceData: ByteArray
}