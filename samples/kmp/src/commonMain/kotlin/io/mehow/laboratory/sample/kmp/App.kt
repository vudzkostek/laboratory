package io.mehow.laboratory.sample.kmp

import io.mehow.laboratory.FeatureFactory
import io.mehow.laboratory.Laboratory
import io.mehow.laboratory.Storage
import io.mehow.laboratory.inspector.cmp.LaboratoryConfiguration

/**
 * Builds a [Laboratory] wired to the generator-emitted `FeatureFactory`. The factory is required
 * on iOS so non-reified lookups (such as those the inspector performs over `KClass`) resolve
 * without JVM reflection.
 */
internal fun createLaboratory(storage: Storage): Laboratory =
  Laboratory.builder()
    .localStorage(storage)
    .featureFactory(FeatureFactory.generated())
    .build()

/** Builds a [LaboratoryConfiguration] for the Compose Multiplatform inspector. */
internal fun createInspectorConfiguration(laboratory: Laboratory): LaboratoryConfiguration =
  LaboratoryConfiguration.create(
    laboratory = laboratory,
    featureFactory = FeatureFactory.generated(),
    sectionName = "Features",
  )
