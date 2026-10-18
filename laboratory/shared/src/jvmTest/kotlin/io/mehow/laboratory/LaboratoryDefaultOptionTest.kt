package io.mehow.laboratory

import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LaboratoryDefaultOptionTest {
  val factory =
    object : DefaultOptionFactory {
      override fun create(feature: Feature<*>) =
        when (feature) {
          is FeatureA -> FeatureA.C
          is FeatureB -> FeatureA.C // Intentionally wrong class
          else -> null
        }
    }
  val laboratory =
    Laboratory.builder().localStorage(Storage.inMemory()).defaultOptionFactory(factory).build()

  @Test
  fun overrideDefaultOption() = runTest {
    laboratory.experiment<FeatureA>() shouldBe FeatureA.C
  }

  @Test
  fun doNotOverrideChangedOption() = runTest {
    for (option in FeatureA::class.java.enumConstants) {
      laboratory.localStorage().setOptions(option)

      laboratory.experiment<FeatureA>() shouldBe option
    }
  }

  @Test
  fun overrideDefaultOptionInFlow() = runTest {
    laboratory.observe<FeatureA>().test {
      awaitItem() shouldBe FeatureA.C

      laboratory.localStorage().setOptions(FeatureA.B)
      awaitItem() shouldBe FeatureA.B
    }
  }

  @Test
  fun failWhenProvidedDefaultOptionUsesWrongType() = runTest {
    val exception = shouldThrow<IllegalStateException> { laboratory.experiment<FeatureB>() }
    exception shouldHaveMessage
      "Tried to use FeatureA.C as a default option for io.mehow.laboratory.FeatureB"
  }
}
