plugins {
  id("com.android.library")
  kotlin("android")
  kotlin("kapt")
}

dependencies {
  api(project(":library:laboratory"))
  api(project(":library:subject-factory"))
  api(Libs.HyperionPlugin)
  implementation(Libs.Kotlin.StdLibJdk7)
  implementation(Libs.AndroidX.CoreKtx)
  implementation(Libs.Material)
  kapt(Libs.AutoService)

  testImplementation(Libs.Kotest.RunnerJunit5)
  testImplementation(Libs.Kotest.Assertions)
}
