package io.mehow.laboratory

import app.cash.turbine.test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LaboratoryTest {
  val laboratory =
    Laboratory.builder().localStorage(Storage.inMemory()).remoteStorage(Storage.inMemory()).build()

  @Test
  fun changeFeatureOption() = runTest {
    for (option in FeatureA::class.java.enumConstants) {
      laboratory.localStorage().setOptions(option)

      laboratory.experiment<FeatureA>() shouldBe option
    }
  }

  @Test
  fun changeMultipleFeatureOptions() = runTest {
    laboratory.localStorage().setOptions(FeatureA.C, FeatureB.B)

    laboratory.experiment<FeatureA>() shouldBe FeatureA.C
    laboratory.experiment<FeatureB>() shouldBe FeatureB.B
  }

  @Test
  fun readBinaryFeature() = runTest {
    laboratory.isEnabled<EnabledBinaryFeature>().shouldBeTrue()
    laboratory.isEnabled<DisabledBinaryFeature>().shouldBeFalse()

    laboratory.setOptions(EnabledBinaryFeature.Disabled, DisabledBinaryFeature.Enabled)

    laboratory.isEnabled<EnabledBinaryFeature>().shouldBeFalse()
    laboratory.isEnabled<DisabledBinaryFeature>().shouldBeTrue()
  }

  @Test
  fun failToUseFeatureWithNoValues() = runTest {
    val exception =
      shouldThrow<IllegalArgumentException> { laboratory.experiment<FeatureWithoutValues>() }
    exception shouldHaveMessage
      "io.mehow.laboratory.FeatureWithoutValues must have at least one option"
  }

  @Test
  fun readLocalFeature() = runTest {
    laboratory.experiment<FeatureA>() shouldBe FeatureA.A

    laboratory.localStorage().setOptions(FeatureA.B)
    laboratory.experiment<FeatureA>() shouldBe FeatureA.B
  }

  @Test
  fun readRemoteFeature() = runTest {
    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.A

    laboratory.remoteStorage()?.setOptions(RemoteFeatureA.B)
    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.B
  }

  @Test
  fun changeFeatureToRemote() = runTest {
    laboratory.remoteStorage()?.setOptions(FeatureA.C)
    laboratory.experiment<FeatureA>() shouldBe FeatureA.A

    laboratory.localStorage().setRemoteSource<FeatureA>()
    laboratory.experiment<FeatureA>() shouldBe FeatureA.C
  }

  @Test
  fun changeFeatureToLocal() = runTest {
    laboratory.localStorage().setOptions(RemoteFeatureA.C)
    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.A

    laboratory.localStorage().setLocalSource<RemoteFeatureA>()
    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.C
  }

  @Test
  fun clearLocalFeatures() = runTest {
    laboratory.localStorage().setOptions(FeatureA.C, FeatureB.C)
    laboratory.localStorage().clear()

    laboratory.experiment<FeatureA>() shouldBe FeatureA.A
    laboratory.experiment<FeatureB>() shouldBe FeatureB.B
  }

  @Test
  fun clearRemoteFeatures() = runTest {
    laboratory.remoteStorage()?.setOptions(RemoteFeatureA.C, RemoteFeatureB.C)
    laboratory.remoteStorage()?.clear()

    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.A
    laboratory.experiment<RemoteFeatureB>() shouldBe RemoteFeatureB.B
  }

  @Test
  fun observeFeature() = runTest {
    laboratory.observe<FeatureA>().test {
      awaitItem() shouldBe FeatureA.A

      laboratory.localStorage().setRemoteSource<FeatureA>()
      expectNoEvents()

      laboratory.localStorage().setLocalSource<FeatureA>()
      expectNoEvents()

      laboratory.localStorage().setOptions(FeatureA.B)
      awaitItem() shouldBe FeatureA.B

      laboratory.localStorage().setRemoteSource<FeatureA>()
      awaitItem() shouldBe FeatureA.A

      laboratory.remoteStorage()?.setOptions(FeatureA.C)
      awaitItem() shouldBe FeatureA.C
    }
  }

  @Test
  fun observeBinaryFeature() = runTest {
    laboratory.observeBinary<EnabledBinaryFeature>().test {
      awaitItem().shouldBeTrue()

      laboratory.localStorage().setRemoteSource<EnabledBinaryFeature>()
      expectNoEvents()

      laboratory.localStorage().setLocalSource<EnabledBinaryFeature>()
      expectNoEvents()

      laboratory.setOptions(EnabledBinaryFeature.Disabled)
      awaitItem().shouldBeFalse()

      laboratory.localStorage().setRemoteSource<EnabledBinaryFeature>()
      awaitItem().shouldBeTrue()

      laboratory.remoteStorage()?.setOptions(EnabledBinaryFeature.Disabled)
      awaitItem().shouldBeFalse()
    }
  }
}
