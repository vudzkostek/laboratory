package io.mehow.laboratory

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mehow.laboratory.internal.InternalLaboratoryApi
import kotlin.reflect.KClass
import kotlin.test.Test

@OptIn(InternalLaboratoryApi::class)
class FeatureFactoryTest {
  val firstFactory =
    object : FeatureFactory {
      override fun create() = setOf(FeatureA::class)

      override fun options(feature: KClass<out Feature<*>>): List<Feature<*>>? =
        when (feature) {
          FeatureA::class -> listOf(FeatureA.A, FeatureA.B, FeatureA.C)
          else -> null
        }
    }

  val secondFactory =
    object : FeatureFactory {
      override fun create() = setOf(FeatureB::class)

      override fun options(feature: KClass<out Feature<*>>): List<Feature<*>>? =
        when (feature) {
          FeatureB::class -> listOf(FeatureB.A, FeatureB.B, FeatureB.C)
          else -> null
        }
    }

  @Test
  fun combinedFactoryReturnsAllFeatures() {
    val factory = firstFactory + secondFactory

    factory.create() shouldContainExactlyInAnyOrder setOf(FeatureA::class, FeatureB::class)
  }

  @Test
  fun combinedFactoryReturnsOptionsFromEitherSide() {
    val factory = firstFactory + secondFactory

    factory.options(FeatureA::class) shouldBe listOf(FeatureA.A, FeatureA.B, FeatureA.C)
    factory.options(FeatureB::class) shouldBe listOf(FeatureB.A, FeatureB.B, FeatureB.C)
    factory.options(FeatureC::class).shouldBeNull()
  }

  @Test
  fun defaultFactoryOptionsReturnsNull() {
    val factory =
      object : FeatureFactory {
        override fun create() = emptySet<KClass<out Feature<*>>>()
      }

    factory.options(FeatureA::class).shouldBeNull()
  }
}
