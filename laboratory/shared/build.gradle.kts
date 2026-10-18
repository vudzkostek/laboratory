plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.agp.kotlin.multiplatform.library)
  // CocoaPods ships with the Kotlin Multiplatform plugin; apply it without a version so Gradle
  // does not try to resolve a separate release that would clash with KMP's bundled copy.
  kotlin("native.cocoapods")
  alias(libs.plugins.maven.publish)
  alias(libs.plugins.dokka)
  alias(libs.plugins.wire)
  alias(libs.plugins.laboratory.convention)
}

wire {
  // Wire's KMP mode wires generated sources into commonMain automatically.
  // We also point the sourcePath at the KMP-style proto location.
  sourcePath { srcDir("src/commonMain/proto") }
  kotlin {}
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

  cocoapods {
    summary = "Laboratory feature-flags SDK"
    homepage = "https://github.com/MiSikora/laboratory"
    version = project.version.toString()
    ios.deploymentTarget = "14.0"
    framework {
      baseName = "Laboratory"
      isStatic = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(libs.coroutines.core)
        api(libs.androidx.datastore)
        api(libs.wire)
        implementation(libs.okio)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.coroutines.test)
        implementation(libs.turbine)
        implementation(libs.kotest.assertions)
        implementation(libs.okio.fakefilesystem)
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
