rootProject.name = "laboratory-root"

pluginManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
}

includeBuild("build-logic")

include(
  ":laboratory:shared",
  ":laboratory:generator",
  ":laboratory:gradle-plugin",
  ":laboratory:inspector",
  ":laboratory:inspector-cmp",
  ":laboratory:hyperion-plugin",
  ":laboratory:testing",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
