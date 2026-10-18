package io.mehow.laboratory

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.test.Test

class FeatureFactoryTest {
  val firstFactory =
    object : FeatureFactory {
      override fun create() = setOf(FeatureA::class)
    }

  val secondFactory =
    object : FeatureFactory {
      override fun create() = setOf(FeatureB::class)
    }

  @Test
  fun combinedFactoryReturnsAllFeatures() {
    val factory = firstFactory + secondFactory

    factory.create() shouldContainExactlyInAnyOrder setOf(FeatureA::class, FeatureB::class)
  }
}
