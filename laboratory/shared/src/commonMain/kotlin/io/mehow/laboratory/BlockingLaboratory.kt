package io.mehow.laboratory

/**
 * A blocking equivalent of [Laboratory] that provides synchronous versions of feature flag
 * operations. On the JVM (and Android) each call runs the suspending work on the current thread via
 * `kotlinx.coroutines.runBlocking`. On Kotlin/Native targets (iOS) the operations throw — callers
 * on iOS should use the suspending [Laboratory] APIs directly.
 */
public class BlockingLaboratory internal constructor(private val laboratory: Laboratory) {
  /** The blocking equivalent of [Laboratory.experiment]. */
  @BlockingIoCall
  public inline fun <reified T> experiment(): T where T : Feature<T>, T : Enum<out T> =
    experiment(T::class)

  /** The blocking equivalent of [Laboratory.experiment]. */
  @BlockingIoCall
  public fun <T> experiment(feature: kotlin.reflect.KClass<out T>): T
    where T : Feature<T>, T : Enum<out T> = runBlockingPlatform { laboratory.experiment(feature) }

  /** The blocking equivalent of [Laboratory.experimentIs]. */
  @BlockingIoCall
  public fun <T> experimentIs(option: T): Boolean where T : Feature<T>, T : Enum<out T> =
    runBlockingPlatform {
      laboratory.experimentIs(option)
    }

  /** The blocking equivalent of [Laboratory.isEnabled]. */
  @BlockingIoCall
  public inline fun <reified T> isEnabled(): Boolean
    where T : BinaryFeature<T>, T : Feature<T>, T : Enum<out T> = isEnabled(T::class)

  /** The blocking equivalent of [Laboratory.isEnabled]. */
  @BlockingIoCall
  public fun <T> isEnabled(feature: kotlin.reflect.KClass<out T>): Boolean
    where T : BinaryFeature<T>, T : Feature<T>, T : Enum<out T> = runBlockingPlatform {
    laboratory.isEnabled(feature)
  }

  /** The blocking equivalent of [Laboratory.setOption]. */
  @BlockingIoCall
  public fun setOption(option: Feature<*>): Boolean = runBlockingPlatform {
    laboratory.setOption(option)
  }

  /** The blocking equivalent of [Laboratory.setOptions]. */
  @BlockingIoCall
  public fun setOptions(vararg options: Feature<*>): Boolean = runBlockingPlatform {
    laboratory.setOptions(*options)
  }

  /** The blocking equivalent of [Laboratory.setOptions]. */
  @BlockingIoCall
  public fun setOptions(options: Collection<Feature<*>>): Boolean = runBlockingPlatform {
    laboratory.setOptions(options)
  }

  /** The blocking equivalent of [Laboratory.clear]. */
  @BlockingIoCall public fun clear(): Boolean = runBlockingPlatform { laboratory.clear() }
}

internal expect fun <T> runBlockingPlatform(block: suspend () -> T): T
