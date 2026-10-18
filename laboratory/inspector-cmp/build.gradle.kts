import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.agp.kotlin.multiplatform.library)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.maven.publish)
  alias(libs.plugins.dokka)
  alias(libs.plugins.laboratory.convention)
}

// Compose Multiplatform 1.13.0-alpha02 is in a mid-transition between its own JB-ported androidx
// klibs and Google's new KMP androidx packages. On every :inspector-cmp compile, that produces
// `KLIB resolver: The same 'unique_name=...' found in more than one library` warnings for
// annotation / collection / lifecycle / savedstate / navigationevent. The convention plugin treats
// warnings as errors, so we locally un-promote them here. Revisit when Compose MP settles on a
// single androidx coordinate (expected around 1.13.x stable); drop this override at that point.
tasks.withType<KotlinCompilationTask<KotlinCommonCompilerOptions>>().configureEach {
  compilerOptions { allWarningsAsErrors.set(false) }
}

kotlin {
  jvm()
  android {
    namespace = "io.mehow.laboratory.inspector.cmp"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()
    minSdk = libs.versions.android.min.sdk.get().toInt()
    withHostTestBuilder {}
  }
  // Compose Multiplatform 1.12 dropped iosX64 (Intel simulator); keep the device + Apple-silicon
  // simulator targets, which match the devices modern consumers actually ship/test on.
  iosArm64()
  iosSimulatorArm64()

  sourceSets {
    val commonMain by getting {
      dependencies {
        api(projects.laboratory.shared)
        implementation(compose.runtime)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(compose.ui)
        implementation(libs.coroutines.core)
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.coroutines.test)
        implementation(libs.kotest.assertions)
      }
    }
  }
}
