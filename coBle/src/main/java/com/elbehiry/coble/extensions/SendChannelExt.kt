package com.elbehiry.coble.extensions

import kotlinx.coroutines.channels.SendChannel

// https://github.com/Kotlin/kotlinx.coroutines/issues/974
internal fun <E> SendChannel<E>.offerSafely(element: E) {
    runCatching {
        trySend(element)
    }
}
