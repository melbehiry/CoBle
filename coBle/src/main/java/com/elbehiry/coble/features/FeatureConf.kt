package com.elbehiry.coble.features

import android.content.Context


interface FeatureConf {
    val id: String

    fun create(context: Context)
    fun stop()
}