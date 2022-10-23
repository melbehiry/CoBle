package com.elbehiry.coble.features.usability

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.location.LocationManager
import com.elbehiry.coble.features.FeatureConf

@Volatile
private var delegate: BluetoothUsabilityFeatureImpl? = null

interface BluetoothUsabilityFeature {

    val bluetoothUsability: BluetoothUsability

    companion object Instance : BluetoothUsabilityFeature, FeatureConf {

        override val id: String = BluetoothUsabilityFeatureImpl.ID

        override val bluetoothUsability: BluetoothUsability
            get() = delegate?.bluetoothUsability
                ?: throw BluetoothUsabilityFeatureInitializedException()


        override fun create(context: Context) {
            if (delegate != null) {
                return
            }

            synchronized(this) {
                if (delegate != null) {
                    return
                }

                val bluetoothManager: BluetoothManager =
                    context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

                val locationManager: LocationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

                val bluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter
                val bluetoothStates: BluetoothStates =
                    BluetoothStates.create(context, bluetoothAdapter)

                val bluetoothPermissions: BluetoothPermissions =
                    BluetoothPermissions.create(context)

                val locationStates: LocationStates = LocationStates.create(context, locationManager)
                val bluetoothUsability: BluetoothUsability = BluetoothUsability.create(
                    bluetoothStates, locationStates, bluetoothPermissions
                )

                delegate = BluetoothUsabilityFeatureImpl(
                    bluetoothUsability = bluetoothUsability
                )
            }
        }

        override fun stop() {}
    }
}

internal class BluetoothUsabilityFeatureImpl(override val bluetoothUsability: BluetoothUsability) :
    BluetoothUsabilityFeature {

    companion object {
        internal const val ID: String = "BluetoothUsability"
    }
}

private class BluetoothUsabilityFeatureInitializedException :
    RuntimeException("BluetoothUsabilityFeature must be added with the CoBle sdk configuration")