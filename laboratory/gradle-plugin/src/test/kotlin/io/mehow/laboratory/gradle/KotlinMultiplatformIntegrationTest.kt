package io.mehow.laboratory.gradle

import io.kotest.matchers.shouldBe
import io.mehow.laboratory.gradle.test.LaboratoryTask
import io.mehow.laboratory.gradle.test.cleanBuildResults
import io.mehow.laboratory.gradle.test.toFixture
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.Before
import org.junit.Test

class KotlinMultiplatformIntegrationTest {
  private val gradleRunner = GradleRunner.create().withPluginClasspath()

  @Before
  fun setUp() {
    cleanBuildResults()
  }

  @Test
  fun `registered factory in a kmp project generates into commonMain`() {
    val fixture = "integration-kotlin-multiplatform".toFixture()
    val result = fixture.buildAll(gradleRunner)

    result[LaboratoryTask.FeatureFlags] shouldBe SUCCESS
    result[LaboratoryTask.OptionFactory] shouldBe SUCCESS
    result[LaboratoryTask.FeatureFactory] shouldBe SUCCESS

    result shouldGenerateFeature "Feature"
    val featureFactory = result.featureFactory("GeneratedFeatureFactory")
    featureFactory shouldCreateFeature "Feature"
    val optionFactory = result.optionFactory("GeneratedOptionFactory")
    optionFactory shouldCreateOptionsFor "Feature"
  }
}
