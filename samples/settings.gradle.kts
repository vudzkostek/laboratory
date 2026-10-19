rootProject.name = "samples-root"

pluginManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

sealed class Dependency {
  abstract val alias: String
  abstract val artifactId: String
  abstract val projectId: String

  val groupId = "io.github.vudzkostek"

  abstract fun appendTo(builder: VersionCatalogBuilder)

  fun substituteIn(substitutions: DependencySubstitutions) {
    with(substitutions) { substitute(module("$groupId:$artifactId")).using(project(projectId)) }
  }
}

class Lib(
  override val alias: String,
  override val artifactId: String,
  override val projectId: String,
) : Dependency() {
  override fun appendTo(builder: VersionCatalogBuilder) {
    builder.library(alias, "$groupId:$artifactId:+")
  }
}

class Plugin(
  override val alias: String,
  override val artifactId: String,
  override val projectId: String,
  val pluginId: String,
) : Dependency() {
  override fun appendTo(builder: VersionCatalogBuilder) {
    builder.plugin(pluginId, "io.github.vudzkostek.laboratory").version("+")
  }
}

val dependencies =
  listOf(
    Lib(alias = "laboratory-runtime", artifactId = "laboratory", projectId = ":laboratory:shared"),
    Lib(
      alias = "laboratory-generator",
      artifactId = "laboratory-generator",
      projectId = ":laboratory:generator",
    ),
    Lib(
      alias = "laboratory-inspector",
      artifactId = "laboratory-inspector",
      projectId = ":laboratory:inspector",
    ),
    Lib(
      alias = "laboratory-hyperionPlugin",
      artifactId = "laboratory-hyperion-plugin",
      projectId = ":laboratory:hyperion-plugin",
    ),
    Lib(
      alias = "laboratory-inspectorCmp",
      artifactId = "inspector-cmp",
      projectId = ":laboratory:inspector-cmp",
    ),
    Plugin(
      alias = "laboratory-gradlePlugin",
      artifactId = "laboratory-gradle-plugin",
      projectId = ":laboratory:gradle-plugin",
      pluginId = "laboratory",
    ),
  )

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
      dependencies.forEach { dependency -> dependency.appendTo(this) }
    }
  }

  repositories {
    mavenCentral()
    google()
  }
}

includeBuild("..") {
  dependencySubstitution { dependencies.forEach { dependency -> dependency.substituteIn(this) } }
}

include(
  ":basic",
  ":default-option",
  ":kmp",
  ":multi-module",
  ":multi-module:multi-module-a",
  ":multi-module:multi-module-b",
  ":multi-module:multi-module-c",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
