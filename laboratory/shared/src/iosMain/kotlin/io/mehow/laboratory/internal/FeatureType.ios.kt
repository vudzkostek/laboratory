package io.mehow.laboratory.internal

import io.mehow.laboratory.Feature
import kotlin.reflect.KClass

// TODO Phase 5: wire a generator-emitted KClass -> List<Feature<*>> registry (via
// FeatureFactory) so non-reified KClass-based lookups work on iOS without reflection. Until then,
// iOS consumers must use the reified entry points `experiment<T>()` / `observe<T>()`, which route
// through `enumValues<T>()` and never hit this bridge.
@InternalLaboratoryApi
public actual fun KClass<out Feature<*>>.optionsRaw(): List<Feature<*>> =
  throw NotImplementedError(
    "Enum option discovery for a non-reified KClass is not available on iOS; use the reified " +
      "experiment<T>() / observe<T>() APIs. See TODO Phase 5 in FeatureType.ios.kt."
  )
