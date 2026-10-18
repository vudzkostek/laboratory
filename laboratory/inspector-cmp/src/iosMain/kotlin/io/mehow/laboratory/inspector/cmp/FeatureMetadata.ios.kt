package io.mehow.laboratory.inspector.cmp

import io.mehow.laboratory.Feature
import kotlin.reflect.KClass

/**
 * iOS deprecation lookup is a Phase 6 polish target.
 *
 * There is no cross-platform way to read a Kotlin `@Deprecated` annotation off a [KClass] on
 * Kotlin/Native. Phase 6 will teach the generator to emit a static list of deprecated features into
 * the generated `FeatureFactory`, which the inspector can then consult without reflection. Until
 * then this bridge pretends there are no deprecations, which is accurate for most consumers.
 */
internal actual fun deprecationLevel(type: KClass<out Feature<*>>): DeprecationLevel? = null
