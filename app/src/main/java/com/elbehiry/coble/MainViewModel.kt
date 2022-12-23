package com.elbehiry.coble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elbehiry.coble.data.DevicesRepository
import com.elbehiry.coble.features.usability.BluetoothUsability
import com.elbehiry.coble.scanner.BluetoothScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class MainViewModel @Inject constructor(
    private val bluetoothUsability: BluetoothUsability,
    private val devicesRepository: DevicesRepository
) : ViewModel() {

    private val _errorMessage = Channel<String>(1, BufferOverflow.DROP_LATEST)
    val errorMessage: Flow<String> =
        _errorMessage.receiveAsFlow().shareIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT.inWholeMilliseconds)
        )

    private val startEvent = MutableSharedFlow<Unit>(replay = 1)

    val sideEffects = bluetoothUsability.sideEffects()
        .onEach { sideEffect ->
            if (sideEffect == BluetoothUsability.SideEffect.UseBluetooth) {
                startEvent.emit(Unit)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT.inWholeMilliseconds),
            initialValue = null
        ).filterNotNull()

    val scanning: SharedFlow<BluetoothScanner.Result> = startEvent.flatMapLatest {
        devicesRepository.startScanning().onEach {
            if (it is BluetoothScanner.Result.Error) {
                _errorMessage.trySend(it.error.message ?: "")
            }
        }.filter {
            it is BluetoothScanner.Result.Success
        }.filterNotNull()
    }.shareIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT.inWholeMilliseconds)
    )

    fun tryUsingBluetooth() {
        viewModelScope.launch {
            bluetoothUsability.checkUsability()
        }
    }

    fun scan() {
        viewModelScope.launch {
            startEvent.emit(Unit)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(bluetoothDevice: BluetoothDevice) {
        viewModelScope.launch {
            val deviceConnectionState = devicesRepository.connectDevice(bluetoothDevice)
            Log.d(
                "MainViewModel",
                "Connection on ${bluetoothDevice.name} changed from ${deviceConnectionState.status} to ${deviceConnectionState.newState}"
            )
        }
    }

    companion object {
        private val SUBSCRIBE_TIMEOUT = 5.toDuration(DurationUnit.SECONDS)
    }
}