package io.mehow.laboratory

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Opt-in annotation indicating that the annotated function may block the calling thread with I/O
 * operations. Use of any API marked with `@BlockingIoCall` requires explicit opt-in, either via
 * `@OptIn(BlockingIoCall::class)` on the calling code or by enabling the opt-in flag for this
 * annotation at compile time.
 *
 * This annotation is applied to blocking feature flag operations to signal that they perform
 * potentially slow, blocking work. It's recommended to use the suspend or asynchronous alternatives
 * when possible instead of relying on blocking calls.
 */
@RequiresOptIn(
  message =
    "This API may block a thread with I/O operations. Opt-in if you intend to allow blocking or use a suspending alternative."
)
@Retention(BINARY)
@Target(FUNCTION)
public annotation class BlockingIoCall
