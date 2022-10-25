package com.elbehiry.coble.scanner

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanSettings
import java.util.regex.Pattern

@DslMarker
annotation class ScanningConfigDsl

@ScanningConfigDsl
inline fun createScanningConfig(block: ScanningConfig.Builder.() -> Unit): ScanningConfig =
    ScanningConfig.Builder().apply(block).build()

class ScanningConfig private constructor(builder: Builder)  {

    val namePatterns: List<Pattern> = builder.namePatterns

    val scanFilters: List<ScanFilter> = builder.scanFilters

    val scanSettings: ScanSettings = builder.scanSettings

    @ScanningConfigDsl
    class Builder {

        var scanSettings: ScanSettings = ScanSettings.Builder().build()

        var scanFilters: List<ScanFilter> = emptyList()

        var namePatterns: List<Pattern> = emptyList()

        fun setScanSetting(settings: ScanSettings) = apply {
            scanSettings = settings
        }

        fun setScanFilters(filters: List<ScanFilter>) = apply {
            scanFilters = filters
        }

        fun setNamePatterns(nameRegexPatterns: List<Pattern>) = apply {
            namePatterns = nameRegexPatterns
        }

        fun build(): ScanningConfig = ScanningConfig(this)
    }
}
