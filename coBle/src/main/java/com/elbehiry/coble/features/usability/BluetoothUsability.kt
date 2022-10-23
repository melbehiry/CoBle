package com.elbehiry.coble.features.usability

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import com.elbehiry.coble.features.usability.BluetoothUsability.SideEffect


internal fun BluetoothUsability.Companion.create(
    bluetoothStates: BluetoothStates,
    locationStates: LocationStates,
    bluetoothPermissions: BluetoothPermissions
): BluetoothUsability = DefaultBluetoothUsability(
    bluetoothStates = bluetoothStates,
    locationStates = locationStates,
    bluetoothPermissions = bluetoothPermissions
)

interface BluetoothUsability {

    fun sideEffects(): Flow<SideEffect>
    suspend fun checkUsability()

    sealed class SideEffect {
        object Initial : SideEffect()
        object UseBluetooth : SideEffect()
        data class RequestPermissions(
            val permissions: List<String>,
            val numTimesRequestedPreviously: Int
        ) : SideEffect()

        data class AskToTurnBluetoothOn(val numTimesAskedPreviously: Int) : SideEffect()
        data class AskToTurnLocationOn(val numTimesAskedPreviously: Int) : SideEffect()
    }

    companion object
}

internal class DefaultBluetoothUsability(
    private val bluetoothStates: BluetoothStates,
    private val locationStates: LocationStates,
    private val bluetoothPermissions: BluetoothPermissions
) : BluetoothUsability {

    private val events = MutableSharedFlow<Unit>()
    private var requestPermissionsCount = 0
    private var askToTurnBluetoothOnCount = 0
    private var askToTurnLocationOnCount = 0


    override fun sideEffects(): Flow<SideEffect> {
        return events.onStart { emit(Unit) }
            .flatMapLatest {
                combine(
                    bluetoothStates.states(),
                    locationStates.states()
                ) { bluetoothState, locationState ->
                    val permissionState = bluetoothPermissions.state()
                    when {
                        permissionState is BluetoothPermissions.State.MissingPermissions ->
                            SideEffect.RequestPermissions(
                                permissionState.permissions,
                                requestPermissionsCount++
                            )
                        bluetoothState is BluetoothState.On -> SideEffect.AskToTurnBluetoothOn(
                            askToTurnBluetoothOnCount++
                        )
                        locationState is LocationState.Off -> SideEffect.AskToTurnLocationOn(
                            askToTurnLocationOnCount++
                        )

                        else -> SideEffect.UseBluetooth
                    }
                }
            }.onStart { emit(SideEffect.Initial) }
    }

    override suspend fun checkUsability() {
        events.emit(Unit)
    }
}
