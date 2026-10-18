package io.mehow.laboratory

import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking

/**
 * A blocking equivalent of [Laboratory] that provides synchronous versions of feature flag
 * operations. Each function in this class will execute the corresponding [Laboratory] suspend
 * function on the current thread, potentially performing I/O and thus **blocking** the thread. This
 * is useful in contexts where suspending functions cannot be used, but should be avoided on main/UI
 * threads.
 */
public class BlockingLaboratory internal constructor(private val laboratory: Laboratory) {
  /** The blocking equivalent of [Laboratory.experiment]. */
  @BlockingIoCall
  public inline fun <reified T> experiment(): T where T : Feature<T>, T : Enum<out T> =
    experiment(T::class)

  /** The blocking equivalent of [Laboratory.experiment]. */
  @BlockingIoCall
  public fun <T> experiment(feature: KClass<out T>): T where T : Feature<T>, T : Enum<out T> =
    runBlocking {
      laboratory.experiment(feature)
    }

  /** The blocking equivalent of [Laboratory.experimentIs]. */
  @BlockingIoCall
  public fun <T> experimentIs(option: T): Boolean where T : Feature<T>, T : Enum<out T> =
    runBlocking {
      laboratory.experimentIs(option)
    }

  /** The blocking equivalent of [Laboratory.isEnabled]. */
  @BlockingIoCall
  public inline fun <reified T> isEnabled(): Boolean
    where T : BinaryFeature<T>, T : Feature<T>, T : Enum<out T> = isEnabled(T::class)

  /** The blocking equivalent of [Laboratory.isEnabled]. */
  @BlockingIoCall
  public fun <T> isEnabled(feature: KClass<out T>): Boolean
    where T : BinaryFeature<T>, T : Feature<T>, T : Enum<out T> = runBlocking {
    laboratory.isEnabled(feature)
  }

  /** The blocking equivalent of [Laboratory.setOption]. */
  @BlockingIoCall
  public fun setOption(option: Feature<*>): Boolean = runBlocking { laboratory.setOption(option) }

  /** The blocking equivalent of [Laboratory.setOptions]. */
  @BlockingIoCall
  public fun setOptions(vararg options: Feature<*>): Boolean = runBlocking {
    laboratory.setOptions(*options)
  }

  /** The blocking equivalent of [Laboratory.setOptions]. */
  @BlockingIoCall
  public fun setOptions(options: Collection<Feature<*>>): Boolean = runBlocking {
    laboratory.setOptions(options)
  }

  /** The blocking equivalent of [Laboratory.clear]. */
  @BlockingIoCall public fun clear(): Boolean = runBlocking { laboratory.clear() }
}

/**
 * Returns a [BlockingLaboratory] that provides blocking (synchronous) versions of this Laboratory's
 * API. Only available on the JVM; iOS consumers should rely on the suspending API surface instead.
 */
public fun Laboratory.blocking(): BlockingLaboratory = BlockingLaboratory(this)
