import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.agp.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  // CocoaPods ships with the KMP plugin — apply without a version so it uses the bundled copy.
  kotlin("native.cocoapods")
  alias(libs.plugins.laboratory)
}

// The KMP sample's :inspector-cmp dependency inherits the Compose Multiplatform 1.12.x warnings
// about duplicate androidx klibs (JB `savedstate` 1.3.6 and Google's `androidx.savedstate` 1.4.0).
// Drop this override once Compose MP settles on one coordinate (expected around 1.13.x stable).
tasks.withType<KotlinCompilationTask<KotlinCommonCompilerOptions>>().configureEach {
  compilerOptions { allWarningsAsErrors.set(false) }
}

laboratory {
  packageName = "io.mehow.laboratory.sample.kmp"

  featureFactory()

  feature("Theme") {
    withDefaultOption("System")
    withOption("Light")
    withOption("Dark")
  }

  disabledFeature("DebugMenu") {
    description = "Shows an in-app debug menu for development builds."
  }

  feature("Analytics") {
    withDefaultOption("Enabled")
    withOption("Disabled")
  }
}

kotlin {
  // KMP + AGP KMP library DSL; matches `:laboratory:shared` and `:laboratory:inspector-cmp`.
  android {
    namespace = "io.mehow.laboratory.sample.kmp"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    minSdk = libs.versions.android.min.sdk.get().toInt()
    withHostTestBuilder {}
  }
  // Compose Multiplatform 1.12 dropped iosX64; target the modern devices + simulators.
  iosArm64()
  iosSimulatorArm64()

  cocoapods {
    summary = "Laboratory KMP sample"
    homepage = "https://github.com/MiSikora/laboratory"
    version = "0.0.1"
    ios.deploymentTarget = "14.0"
    framework {
      baseName = "LaboratorySample"
      isStatic = true
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        // The laboratory plugin wires `io.mehow.laboratory:laboratory` to commonMain for us; we
        // only have to pull in the inspector + Compose MP ourselves.
        implementation(libs.laboratory.inspectorCmp)
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(libs.coroutines.core)
      }
    }
  }
}
