package io.mehow.laboratory

import io.kotest.matchers.shouldBe
import kotlin.test.Test

@OptIn(BlockingIoCall::class)
class BlockingLaboratoryTest {
  val laboratory = Laboratory.builder().localStorage(Storage.inMemory()).build().blocking()

  @Test
  fun readDefaultOption() {
    laboratory.experiment<FeatureA>() shouldBe FeatureA.A
  }

  @Test
  fun setAndReadOption() {
    laboratory.setOption(FeatureA.B)

    laboratory.experiment<FeatureA>() shouldBe FeatureA.B
  }

  @Test
  fun experimentIsMatchesCurrentOption() {
    laboratory.setOption(FeatureA.C)

    laboratory.experimentIs(FeatureA.C) shouldBe true
    laboratory.experimentIs(FeatureA.A) shouldBe false
  }

  @Test
  fun isEnabledForBinaryFeature() {
    laboratory.isEnabled<EnabledBinaryFeature>() shouldBe true

    laboratory.setOption(EnabledBinaryFeature.Disabled)
    laboratory.isEnabled<EnabledBinaryFeature>() shouldBe false
  }

  @Test
  fun clearRemovesOptions() {
    laboratory.setOption(FeatureA.C)
    laboratory.clear()

    laboratory.experiment<FeatureA>() shouldBe FeatureA.A
  }
}
