package io.mehow.laboratory

import io.kotest.matchers.shouldBe
import io.mehow.laboratory.internal.InternalLaboratoryApi
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

@OptIn(InternalLaboratoryApi::class)
class LaboratoryFeatureFactoryTest {
  private val registry =
    object : FeatureFactory {
      override fun create(): Set<KClass<out Feature<*>>> =
        setOf(
          FeatureA::class,
          FeatureB::class,
          FeatureC::class,
          RemoteFeatureA::class,
          RemoteFeatureB::class,
          EnabledBinaryFeature::class,
          DisabledBinaryFeature::class,
        )

      override fun options(feature: KClass<out Feature<*>>): List<Feature<*>>? =
        when (feature) {
          FeatureA::class -> listOf(FeatureA.A, FeatureA.B, FeatureA.C)
          FeatureB::class -> listOf(FeatureB.A, FeatureB.B, FeatureB.C)
          FeatureC::class -> listOf(FeatureC.A, FeatureC.B, FeatureC.C)
          RemoteFeatureA::class -> listOf(RemoteFeatureA.A, RemoteFeatureA.B, RemoteFeatureA.C)
          RemoteFeatureB::class -> listOf(RemoteFeatureB.A, RemoteFeatureB.B, RemoteFeatureB.C)
          EnabledBinaryFeature::class ->
            listOf(EnabledBinaryFeature.Enabled, EnabledBinaryFeature.Disabled)
          DisabledBinaryFeature::class ->
            listOf(DisabledBinaryFeature.Enabled, DisabledBinaryFeature.Disabled)
          else -> null
        }
    }

  private fun laboratory(): Laboratory =
    Laboratory.builder().localStorage(Storage.inMemory()).featureFactory(registry).build()

  @Test
  fun experimentReadsCurrentOption() = runTest {
    val laboratory = laboratory()

    laboratory.setOption(FeatureA.C)
    laboratory.experiment<FeatureA>() shouldBe FeatureA.C
  }

  @Test
  fun experimentFallsBackToDefaultWhenNoOptionSet() = runTest {
    val laboratory = laboratory()

    laboratory.experiment<FeatureB>() shouldBe FeatureB.B
  }

  @Test
  fun registryIsUsedForNonReifiedLookup() = runTest {
    val laboratory = laboratory()

    laboratory.setOption(FeatureC.B)
    laboratory.experiment(FeatureC::class) shouldBe FeatureC.B
  }
}
