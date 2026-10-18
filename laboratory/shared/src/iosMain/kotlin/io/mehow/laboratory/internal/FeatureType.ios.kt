package io.mehow.laboratory.internal

import io.mehow.laboratory.Feature
import kotlin.reflect.KClass

// On iOS, non-reified KClass<out Feature<*>> option lookups cannot use JVM reflection
// (java.lang.Class.enumConstants is unavailable). Register a generator-emitted FeatureFactory via
// Laboratory.Builder.featureFactory(GeneratedFeatureFactory) so the storage layer can resolve
// options without touching this bridge. The reified experiment<T>() / observe<T>() APIs route
// through this bridge too (via KClass<out T>.options()), so this fallback must stay off the hot
// path on iOS.
@InternalLaboratoryApi
public actual fun KClass<out Feature<*>>.optionsRaw(): List<Feature<*>> =
  throw NotImplementedError(
    "Enum option discovery for a non-reified KClass is not available on iOS. Register the " +
      "generator-emitted FeatureFactory via Laboratory.Builder.featureFactory(...) so option " +
      "lookups can go through its options() registry instead of reflection."
  )
