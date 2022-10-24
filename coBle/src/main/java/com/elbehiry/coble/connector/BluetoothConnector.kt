package com.elbehiry.coble.connector

import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.util.UUID

private const val DEFAULT_GATT_TIMEOUT = 5000L
private val ClientCharacteristicConfigurationID =
    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


internal fun BluetoothConnector.Companion.create(): BluetoothConnector =
    AndroidBluetoothConnector()


interface BluetoothConnector {

    suspend fun connect(context: Context, device: BluetoothDevice): ConnectionStateChanged
    suspend fun registerNotifications(
        characteristic: BluetoothGattCharacteristic,
        clientCharacteristicConfigurationID: UUID = ClientCharacteristicConfigurationID
    ): Boolean

    suspend fun discoverServices(): Boolean
    suspend fun getServices(): List<BluetoothGattService>
    suspend fun getService(uuid: UUID): BluetoothGattService
    suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor): DescriptorWritten
    suspend fun readDescriptor(descriptor: BluetoothGattDescriptor): DescriptorRead
    suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic): CharacteristicWritten
    suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic): CharacteristicRead
    suspend fun setPreferredPhy(txPhy: Int, rxPhy: Int, phyOptions: Int): PhyUpdate
    suspend fun release()

    companion object
}

private class AndroidBluetoothConnector : BluetoothConnector {

    private val gattCallback = GattCallback()
    private val mutex = Mutex()
    private var bluetoothGatt: BluetoothGatt? = null

    val events = gattCallback.events
    val characteristicChangedEvents = gattCallback.characteristicChangedEvents
    val device: BluetoothDevice?
        get() = bluetoothGatt?.device

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun connect(
        context: Context,
        device: BluetoothDevice
    ): ConnectionStateChanged =
        mutex.queueWithTimeout("connect") {
            if (bluetoothGatt != null) {
                Timber.d("reconnecting...")
                reconnect(context, device)
            } else {
                Timber.d("new connection")
                connectDevice(device, context)
            }
        }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    private suspend fun connectDevice(
        device: BluetoothDevice,
        context: Context
    ): ConnectionStateChanged {
        val connectionStateChanged = events.onSubscription {
            bluetoothGatt = device.connectGatt(
                context.applicationContext,
                true,
                gattCallback
            )
        }.firstOrNull {
            it is ConnectionStateChanged &&
                    it.status == BluetoothGatt.GATT_SUCCESS &&
                    it.newState == BluetoothProfile.STATE_CONNECTED
        } as ConnectionStateChanged?
            ?: ConnectionStateChanged(
                BluetoothGatt.GATT_FAILURE,
                BluetoothProfile.STATE_DISCONNECTED
            )
        Timber.d(connectionStateChanged.toString())

        return connectionStateChanged
    }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    private suspend fun reconnect(
        context: Context,
        device: BluetoothDevice
    ): ConnectionStateChanged {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val state = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)
        return if (state == BluetoothProfile.STATE_CONNECTED) {
            ConnectionStateChanged(BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        } else {
            events
                .onSubscription { requireGatt().connect() }
                .firstOrNull {
                    it is ConnectionStateChanged &&
                            it.status == BluetoothGatt.GATT_SUCCESS &&
                            it.newState == BluetoothProfile.STATE_CONNECTED
                } as ConnectionStateChanged?
                ?: ConnectionStateChanged(
                    BluetoothGatt.GATT_FAILURE,
                    BluetoothProfile.STATE_DISCONNECTED
                )
        }
    }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun registerNotifications(
        characteristic: BluetoothGattCharacteristic,
        clientCharacteristicConfigurationID: UUID
    ): Boolean {
        var success = false
        while (!success) {
            success = mutex.queueWithTimeout("register notification on ${characteristic.uuid}") {
                requireGatt().setCharacteristicNotification(characteristic, true)
                val result =
                    characteristic.descriptors.find { it.uuid == clientCharacteristicConfigurationID }
                        ?.let { descriptor ->
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            events
                                .onSubscription {
                                    val descriptorWrittenResult =
                                        requireGatt().writeDescriptor(descriptor)
                                    Timber.d("Descriptor ${descriptor.uuid} on ${descriptor.characteristic.uuid} written: $descriptorWrittenResult")
                                    if (!descriptorWrittenResult) {
                                        emit(
                                            DescriptorWritten(
                                                descriptor,
                                                BluetoothGatt.GATT_FAILURE
                                            )
                                        )
                                    }
                                }
                                .firstOrNull {
                                    it is DescriptorWritten &&
                                            it.descriptor.characteristic.uuid == descriptor.characteristic.uuid &&
                                            it.descriptor.uuid == descriptor.uuid
                                } as DescriptorWritten?
                                ?: DescriptorWritten(descriptor, BluetoothGatt.GATT_FAILURE)
                        }
                result?.status == BluetoothGatt.GATT_SUCCESS
            }
        }
        return success
    }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun discoverServices() =
        mutex.queueWithTimeout("discover services") { requireGatt().discoverServices() }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun getServices(): List<BluetoothGattService> =
        mutex.queueWithTimeout("get all services") { requireGatt().services }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun getService(uuid: UUID): BluetoothGattService =
        mutex.queueWithTimeout("get service $uuid") { requireGatt().getService(uuid) }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun writeDescriptor(descriptor: BluetoothGattDescriptor): DescriptorWritten =
        mutex.queueWithTimeout("write descriptor ${descriptor.uuid} on char ${descriptor.characteristic.uuid}") {
            events
                .onSubscription {
                    if (!requireGatt().writeDescriptor(descriptor)) {
                        emit(DescriptorWritten(descriptor, BluetoothGatt.GATT_FAILURE))
                    }
                }
                .firstOrNull {
                    it is DescriptorWritten &&
                            it.descriptor.characteristic.uuid == descriptor.characteristic.uuid &&
                            it.descriptor.uuid == descriptor.uuid
                } as DescriptorWritten?
                ?: DescriptorWritten(descriptor, BluetoothGatt.GATT_FAILURE)
        }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun readDescriptor(descriptor: BluetoothGattDescriptor): DescriptorRead =
        mutex.queueWithTimeout("write descriptor ${descriptor.uuid} on char ${descriptor.characteristic.uuid}") {
            events
                .onSubscription {
                    if (!requireGatt().readDescriptor(descriptor)) {
                        emit(DescriptorRead(descriptor, BluetoothGatt.GATT_FAILURE))
                    }
                }
                .firstOrNull {
                    it is DescriptorRead &&
                            it.descriptor.characteristic.uuid == descriptor.characteristic.uuid &&
                            it.descriptor.uuid == descriptor.uuid
                } as DescriptorRead?
                ?: DescriptorRead(descriptor, BluetoothGatt.GATT_FAILURE)
        }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun writeCharacteristic(characteristic: BluetoothGattCharacteristic): CharacteristicWritten =
        mutex.queueWithTimeout("write characteristic ${characteristic.uuid}") {
            events
                .onSubscription {
                    if (!requireGatt().writeCharacteristic(characteristic)) {
                        emit(CharacteristicWritten(characteristic, BluetoothGatt.GATT_FAILURE))
                    }
                }
                .firstOrNull {
                    it is CharacteristicWritten &&
                            it.characteristic.uuid == characteristic.uuid
                } as CharacteristicWritten?
                ?: CharacteristicWritten(characteristic, BluetoothGatt.GATT_FAILURE)
        }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun readCharacteristic(characteristic: BluetoothGattCharacteristic): CharacteristicRead =
        mutex.queueWithTimeout("read characteristic ${characteristic.uuid}") {
            events
                .onSubscription {
                    if (!requireGatt().readCharacteristic(characteristic)) {
                        emit(CharacteristicRead(characteristic, BluetoothGatt.GATT_FAILURE))
                    }
                }
                .firstOrNull {
                    it is CharacteristicRead &&
                            it.characteristic.uuid == characteristic.uuid
                } as CharacteristicRead?
                ?: CharacteristicRead(characteristic, BluetoothGatt.GATT_FAILURE)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun setPreferredPhy(txPhy: Int, rxPhy: Int, phyOptions: Int): PhyUpdate =
        mutex.queueWithTimeout("set phy to $txPhy, $rxPhy, $phyOptions") {
            events
                .onSubscription { requireGatt().setPreferredPhy(txPhy, rxPhy, phyOptions) }
                .firstOrNull { it is PhyUpdate } as PhyUpdate?
                ?: PhyUpdate(
                    BluetoothDevice.PHY_LE_1M,
                    BluetoothDevice.PHY_LE_1M,
                    BluetoothGatt.GATT_FAILURE
                )
        }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    override suspend fun release() = mutex.queueWithTimeout("release") {
        requireGatt().disconnect()
        requireGatt().close()
        bluetoothGatt = null
    }

    @RequiresPermission(anyOf = [BLUETOOTH, BLUETOOTH_CONNECT])
    private fun requireGatt(): BluetoothGatt =
        bluetoothGatt ?: throw IllegalStateException("BluetoothGatt is null")
}

private suspend fun <T> Mutex.queueWithTimeout(
    action: String,
    timeout: Long = DEFAULT_GATT_TIMEOUT,
    block: suspend CoroutineScope.() -> T
): T {
    return try {
        withLock {
            return@withLock withTimeout(timeMillis = timeout, block = block)
        }
    } catch (e: Exception) {
        Timber.w(e, "Timeout on BLE call: $action")
        throw e
    }
}
