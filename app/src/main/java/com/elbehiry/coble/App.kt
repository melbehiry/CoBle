package com.elbehiry.coble

import android.app.Application
import com.elbehiry.coble.features.usability.BluetoothUsabilityFeature
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = config(context = applicationContext) {
            addFeature(BluetoothUsabilityFeature.Instance)
        }

        CoBle.init(config)
    }
}