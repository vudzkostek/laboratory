package io.mehow.laboratory

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DefaultSourceFactoryTest {
  val firstFactory =
    object : DefaultSourceFactory {
      override fun create(feature: Feature<*>): Feature.Source? =
        when (feature) {
          is FeatureA -> Feature.Source.Remote
          else -> null
        }
    }

  val secondFactory =
    object : DefaultSourceFactory {
      override fun create(feature: Feature<*>): Feature.Source? =
        when (feature) {
          is FeatureA -> Feature.Source.Local
          is FeatureB -> Feature.Source.Remote
          else -> null
        }
    }

  @Test
  fun combinedFactoryUsesFirstFactory() {
    val factory = firstFactory + secondFactory

    factory.create(FeatureA.A) shouldBe Feature.Source.Remote
  }

  @Test
  fun combinedFactoryUsesSecondFactory() {
    val factory = firstFactory + secondFactory

    factory.create(FeatureB.A) shouldBe Feature.Source.Remote
  }

  @Test
  fun combinedFactoryUsesNoFactories() {
    val factory = firstFactory + secondFactory

    factory.create(FeatureC.A) shouldBe null
  }
}
