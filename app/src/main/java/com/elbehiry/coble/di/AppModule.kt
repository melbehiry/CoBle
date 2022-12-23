package com.elbehiry.coble.di

import com.elbehiry.coble.CoBle
import com.elbehiry.coble.connector.BluetoothConnector
import com.elbehiry.coble.data.DevicesRepository
import com.elbehiry.coble.data.ScanningDevicesRepository
import com.elbehiry.coble.features.usability.BluetoothUsability
import com.elbehiry.coble.features.usability.BluetoothUsabilityFeature
import com.elbehiry.coble.scanner.BluetoothScanner
import com.elbehiry.coble.scanner.OneShotBluetoothScanner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideBluetoothConnector(): BluetoothConnector = CoBle.connector

    @Provides
    fun provideBluetoothScanner(): BluetoothScanner = CoBle.scanner

    @Provides
    fun provideBluetoothUsability(): BluetoothUsability =
        BluetoothUsabilityFeature.bluetoothUsability

    @Provides
    fun provideOneShotBluetoothScanner(): OneShotBluetoothScanner = CoBle.oneShotScanner

    @Provides
    fun provideDevicesRepository(
        scanner: BluetoothScanner,
        connector: BluetoothConnector
    ): DevicesRepository = ScanningDevicesRepository(
        scanner = scanner,
        connector = connector
    )
}