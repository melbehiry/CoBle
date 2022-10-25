package com.elbehiry.coble.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import java.util.regex.Pattern

object BleDeviceMatcher {

    @RequiresPermission(anyOf = [Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT])
    fun matchesName(namePattern: Pattern?, device: BluetoothDevice?): Boolean {
        return if (namePattern == null) {
            true
        } else if (device?.name == null) {
            false
        } else {
            val name = device.name
            val matcher = namePattern.matcher(name).find()
            name != null && matcher
        }
    }
}
