package io.mehow.laboratory.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(
  because = "Output is small and fast to regenerate; caching provides no benefit."
)
internal abstract class OutputTask : DefaultTask() {
  @get:Internal abstract val outputDirectory: DirectoryProperty
}
