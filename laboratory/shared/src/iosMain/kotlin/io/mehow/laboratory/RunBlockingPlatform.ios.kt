package io.mehow.laboratory

internal actual fun <T> runBlockingPlatform(block: suspend () -> T): T {
  throw NotImplementedError(
    "Laboratory.blocking() is not supported on iOS. Use the suspending Laboratory API directly."
  )
}
