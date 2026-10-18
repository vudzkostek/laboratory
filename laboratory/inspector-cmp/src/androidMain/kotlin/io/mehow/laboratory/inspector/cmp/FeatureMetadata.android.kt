package io.mehow.laboratory.inspector.cmp

import io.mehow.laboratory.Feature
import kotlin.reflect.KClass

internal actual fun deprecationLevel(type: KClass<out Feature<*>>): DeprecationLevel? =
  type.java.annotations.filterIsInstance<Deprecated>().firstOrNull()?.level
