package com.elbehiry.coble.features.usability

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.elbehiry.coble.extensions.offerSafely
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart

sealed class LocationState {
    object On : LocationState()
    object Off : LocationState()
}

internal fun LocationStates.Companion.create(
    context: Context,
    locationManager: LocationManager
): LocationStates = AndroidLocationStates(
    context = context, locationManager = locationManager
)

interface LocationStates {
    fun states(): Flow<LocationState>

    companion object
}

class AndroidLocationStates(
    private val context: Context,
    private val locationManager: LocationManager
) : LocationStates {
    override fun states(): Flow<LocationState> {
        val locationStates = callbackFlow {
            val broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action != LocationManager.PROVIDERS_CHANGED_ACTION) {
                        return
                    }

                    offerSafely(
                        LocationManagerCompat.isLocationEnabled(locationManager).toLocationState()
                    )
                }
            }

            context.registerReceiver(
                broadcastReceiver,
                IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            )

            awaitClose { context.unregisterReceiver(broadcastReceiver) }
        }

        return locationStates
            .onStart {
                emit(LocationManagerCompat.isLocationEnabled(locationManager).toLocationState())
            }.distinctUntilChanged()
    }

    private fun Boolean.toLocationState(): LocationState {
        return if (this) {
            LocationState.On
        } else {
            LocationState.Off
        }
    }

}