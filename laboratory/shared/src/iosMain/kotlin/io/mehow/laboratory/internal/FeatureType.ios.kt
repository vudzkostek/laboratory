package io.mehow.laboratory.internal

import io.mehow.laboratory.Feature
import kotlin.reflect.KClass

@InternalLaboratoryApi
public actual fun KClass<out Feature<*>>.optionsRaw(): List<Feature<*>> =
  throw NotImplementedError(
    "Enum option discovery for KClass is wired up in Phase 4; use reified APIs on iOS."
  )
