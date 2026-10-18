package io.mehow.laboratory

import kotlinx.coroutines.runBlocking

internal actual fun <T> runBlockingPlatform(block: suspend () -> T): T = runBlocking { block() }
