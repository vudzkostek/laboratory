package io.mehow.laboratory.internal

import io.mehow.laboratory.Feature
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

// TODO Phase 4: replace with a generator-populated KClass -> List<Feature<*>> registry so this
// reflection-based bridge can go away on every platform.
private val cache = atomic<Map<KClass<out Feature<*>>, List<Feature<*>>>>(emptyMap())

@InternalLaboratoryApi
public actual fun KClass<out Feature<*>>.optionsRaw(): List<Feature<*>> {
  cache.value[this]?.let {
    return it
  }
  val qualified = qualifiedName ?: simpleName
  val enumConstants: Array<out Any> =
    requireNotNull(this.java.enumConstants) { "$qualified must be an enum" }
  require(enumConstants.isNotEmpty()) { "$qualified must have at least one option" }
  require(enumConstants.first() is Feature<*>) { "$qualified must be a feature" }
  @Suppress("UNCHECKED_CAST") val options = enumConstants.toList() as List<Feature<*>>
  cache.update { current -> if (this in current) current else current + (this to options) }
  return cache.value[this] ?: options
}
