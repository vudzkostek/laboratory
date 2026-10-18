package io.mehow.laboratory

import app.cash.turbine.test
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class LaboratoryDefaultSourceTest {
  val factory =
    object : DefaultSourceFactory {
      override fun create(feature: Feature<*>) =
        when (feature) {
          is FeatureA -> Feature.Source.Remote
          is RemoteFeatureA -> Feature.Source.Local
          else -> null
        }
    }

  val laboratory =
    Laboratory.builder()
      .localStorage(Storage.inMemory())
      .remoteStorage(Storage.inMemory())
      .defaultSourceFactory(factory)
      .build()

  @Test
  fun overrideDefaultLocalSource() = runTest {
    laboratory.remoteStorage()?.setOptions(FeatureA.C)

    laboratory.experiment<FeatureA>() shouldBe FeatureA.C
  }

  @Test
  fun overrideDefaultRemoteSource() = runTest {
    laboratory.localStorage().setOptions(RemoteFeatureA.C)

    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.C
  }

  @Test
  fun doNotOverrideChangedLocalSource() = runTest {
    laboratory.localStorage().setLocalSource<FeatureA>()
    laboratory.remoteStorage()?.setOptions(FeatureA.C)

    laboratory.experiment<FeatureA>() shouldBe FeatureA.A
  }

  @Test
  fun doNotOverrideChangedRemoteSource() = runTest {
    laboratory.localStorage().setRemoteSource<RemoteFeatureA>()
    laboratory.localStorage().setOptions(RemoteFeatureA.C)

    laboratory.experiment<RemoteFeatureA>() shouldBe RemoteFeatureA.A
  }

  @Test
  fun overrideDefaultSourceInFlow() = runTest {
    laboratory.observe<FeatureA>().test {
      awaitItem() shouldBe FeatureA.A

      laboratory.remoteStorage()?.setOptions(FeatureA.B)
      awaitItem() shouldBe FeatureA.B
    }
  }
}
