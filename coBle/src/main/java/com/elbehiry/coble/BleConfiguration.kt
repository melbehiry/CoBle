package com.elbehiry.coble

import android.content.Context
import com.elbehiry.coble.features.FeatureConf

private const val DEFAULT_SCANNING_TIMEOUT = 15

class BleConfiguration(internal val context: Context, internal val scanningTimeOutInSeconds: Int) {

    val features = mutableMapOf<String, FeatureConf>()

    fun addFeature(feature: FeatureConf) {
        features[feature.id] = feature
    }
}

fun config(
    context: Context,
    scanningTimeOutInSeconds: Int = DEFAULT_SCANNING_TIMEOUT,
    init: BleConfiguration.() -> Unit
): BleConfiguration {
    val conf = BleConfiguration(context, scanningTimeOutInSeconds)
    conf.init()
    return conf
}
