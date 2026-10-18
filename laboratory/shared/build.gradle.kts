plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.agp.kotlin.multiplatform.library)
  alias(libs.plugins.maven.publish)
  alias(libs.plugins.dokka)
  alias(libs.plugins.laboratory.convention)
}

kotlin {
  jvm()
  android {
    namespace = "io.mehow.laboratory.shared"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    minSdk = libs.versions.android.min.sdk.get().toInt()
    withHostTestBuilder {}
    withDeviceTestBuilder { sourceSetTreeName = "test" }
  }
  iosX64()
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    val commonMain by getting {
      dependencies { api(libs.coroutines.core) }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.coroutines.test)
        implementation(libs.turbine)
        implementation(libs.kotest.assertions)
      }
    }
    val jvmMain by getting {
      dependencies { implementation(libs.atomicfu) }
    }
    val jvmTest by getting {
      dependencies { implementation(libs.junit) }
    }
    val androidMain by getting {
      dependencies { implementation(libs.atomicfu) }
    }
  }
}
