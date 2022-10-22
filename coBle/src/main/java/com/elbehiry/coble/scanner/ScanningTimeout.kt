package com.elbehiry.coble.scanner

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun ScanningTimeout.Companion.create(scanningTimeOutInSeconds: Int): ScanningTimeout =
    DefaultScanningTimeout(scanningTimeOutInSeconds)

interface ScanningTimeout {
    val timeout: Duration

    companion object
}

class DefaultScanningTimeout(scanningTimeOutInSeconds: Int) : ScanningTimeout {
    override val timeout: Duration = scanningTimeOutInSeconds.toDuration(DurationUnit.SECONDS)
}