package io.mehow.laboratory

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DefaultOptionFactoryTest {
  val firstFactory =
    object : DefaultOptionFactory {
      override fun create(feature: Feature<*>): Feature<*>? =
        when (feature) {
          is FeatureA -> FeatureA.B
          else -> null
        }
    }

  val secondFactory =
    object : DefaultOptionFactory {
      override fun create(feature: Feature<*>): Feature<*>? =
        when (feature) {
          is FeatureA -> FeatureA.C
          is FeatureB -> FeatureB.C
          else -> null
        }
    }

  @Test
  fun combinedFactoryUsesFirstFactory() {
    val factory = firstFactory + secondFactory

    factory.create(FeatureA.A) shouldBe FeatureA.B
  }

  @Test
  fun combinedFactoryUsesSecondFactory() {
    val factory = firstFactory + secondFactory

    factory.create(FeatureB.A) shouldBe FeatureB.C
  }

  @Test
  fun combinedFactoryUsesNoFactories() {
    val factory = firstFactory + secondFactory

    factory.create(FeatureC.A) shouldBe null
  }
}
