package io.mehow.laboratory.gradle

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Applies the Laboratory Gradle plugin to a project.
 *
 * When applied, the plugin:
 * - Registers the `laboratory` extension used for configuration.
 * - Configures code generation tasks for feature flags and related helpers.
 *
 * A Kotlin plugin (`org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.android`, or
 * `org.jetbrains.kotlin.multiplatform`) must be applied before this plugin.
 */
public class LaboratoryPlugin : Plugin<Project> {
  private val hasKotlin = AtomicBoolean(false)
  private val hasAndroid = AtomicBoolean(false)
  private val hasKmp = AtomicBoolean(false)

  override fun apply(target: Project) {
    val extension = target.extensions.create(PluginName, LaboratoryExtension::class.java)
    val tasks = LaboratoryTasks.registerIn(target)
    tasks.configureInput(target, extension)

    target.withPlugins("org.jetbrains.kotlin.multiplatform") {
      hasKotlin.set(true)
      hasKmp.set(true)
    }

    target.withPlugins(
      "org.jetbrains.kotlin.jvm",
      "org.jetbrains.kotlin.android",
      "com.android.experimental.built-in-kotlin",
    ) {
      hasKotlin.set(true)
    }

    target.withPlugins(
      "com.android.application",
      "com.android.library",
      "com.android.instantapp",
      "com.android.feature",
      "com.android.dynamic-feature",
    ) {
      hasAndroid.set(true)
      target.evaluateKotlinInAndroid()
      target.extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
        target.contributeToSources(tasks, ProjectSources.Android(variant))
      }
    }

    target.afterEvaluate {
      target.contributeToSourcesAfterEvaluation(tasks)
      target.addLaboratoryDependency()
    }
  }

  private fun Project.evaluateKotlinInAndroid() {
    val isPropEnabled = findProperty("android.builtInKotlin")?.toString()?.toBoolean()
    extensions.getByType(AndroidComponentsExtension::class.java).finalizeDsl {
      val commonExt = extensions.getByType(CommonExtension::class.java)
      val isDslEnabled = commonExt.enableKotlin
      hasKotlin.set(isDslEnabled && isPropEnabled != false)
    }
  }

  private fun Project.contributeToSourcesAfterEvaluation(tasks: LaboratoryTasks) {
    check(hasKotlin.get()) { "Laboratory Gradle plugin applied in '$path' requires Kotlin plugin." }
    // Priority: KMP > Android > Kotlin JVM. Android is wired up eagerly in `onVariants`;
    // we only contribute here for KMP (commonMain) and pure Kotlin JVM projects.
    when {
      hasKmp.get() -> tasks.contributeToSources(ProjectSources.KotlinMultiplatform(this))
      hasAndroid.get() -> Unit // Already contributed via onVariants.
      else -> tasks.contributeToSources(ProjectSources.Kotlin(this))
    }
  }

  private fun Project.contributeToSources(tasks: LaboratoryTasks, sources: ProjectSources) {
    // Android variant callback path. Skip if this is a KMP + Android project; the KMP branch
    // contributes to commonMain and we would otherwise double-add sources on the Android variant.
    if (hasKmp.get()) return
    check(hasKotlin.get()) { "Laboratory Gradle plugin applied in '$path' requires Kotlin plugin." }
    tasks.contributeToSources(sources)
  }

  private fun Project.addLaboratoryDependency() {
    // Default coordinate is the JitPack path for the vudzkostek fork. Consumers can override via
    // the `laboratory.libraryCoordinate` project property, e.g. in gradle.properties or on the
    // command line, when they publish Laboratory elsewhere (Maven Central, GitHub Packages, …).
    val coordinate =
      findProperty("laboratory.libraryCoordinate")?.toString()
        ?: "com.github.vudzkostek.laboratory:laboratory:$LibraryVersion"
    if (hasKmp.get()) {
      val extension = extensions.getByType(KotlinMultiplatformExtension::class.java)
      extension.sourceSets.getByName("commonMain").dependencies { api(coordinate) }
    } else {
      dependencies.add("api", coordinate)
    }
  }
}

private fun Project.withPlugins(vararg ids: String, action: (Plugin<*>) -> Unit) {
  for (id in ids) {
    plugins.withId(id, action)
  }
}
