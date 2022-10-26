package com.elbehiry.coble.connector

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.util.Log
import com.elbehiry.coble.LoggingTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

internal class GattCallback : BluetoothGattCallback() {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<GattEvent>(extraBufferCapacity = 100)
    val events: SharedFlow<GattEvent> = _events

    private val _characteristicChangedEvents =
        MutableSharedFlow<CharacteristicChanged>(extraBufferCapacity = 100)
    val characteristicChangedEvents: SharedFlow<CharacteristicChanged> =
        _characteristicChangedEvents

    private fun <T> MutableSharedFlow<T>.emitEvent(event: T) {
        scope.launch { emit(event) }
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if (characteristic != null) {
            _events.emitEvent(CharacteristicWritten(characteristic, status))
        }
    }

    override fun onPhyUpdate(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        _events.emitEvent(PhyUpdate(txPhy, rxPhy, status))
    }

    override fun onPhyRead(gatt: BluetoothGatt?, txPhy: Int, rxPhy: Int, status: Int) {
        _events.emitEvent(PhyRead(txPhy, rxPhy, status))
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        Log.d(LoggingTag,"onConnectionStateChange $status $newState ${stateText[newState]} ")
         _events.emitEvent(ConnectionStateChanged(status, newState))
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        _events.emitEvent(ServicesDiscovered(status))
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        if (characteristic != null) {
            _events.emitEvent(CharacteristicRead(characteristic, status))
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        if (characteristic != null) {
            _characteristicChangedEvents.emitEvent(CharacteristicChanged(characteristic))
        }
    }

    override fun onDescriptorRead(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        if (descriptor != null) {
            _events.emitEvent(DescriptorRead(descriptor, status))
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        if (descriptor != null) {
            _events.emitEvent(DescriptorWritten(descriptor, status))
        }
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt?, status: Int) {
        _events.emitEvent(ReliableWriteCompleted(status))
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        _events.emitEvent(ReadRemoteRssi(rssi, status))
    }

    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        _events.emitEvent(MtuChanged(mtu, status))
    }
}


internal val stateText = mapOf(
    BluetoothProfile.STATE_CONNECTED to "Connected",
    BluetoothProfile.STATE_CONNECTING to "Connecting",
    BluetoothProfile.STATE_DISCONNECTED to "Disconnected",
    BluetoothProfile.STATE_DISCONNECTING to "Disconnecting",
)