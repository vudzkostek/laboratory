package io.mehow.laboratory.shared

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalNativeApi::class)
internal actual fun platform(): String = "ios-${Platform.cpuArchitecture.name.lowercase()}"
