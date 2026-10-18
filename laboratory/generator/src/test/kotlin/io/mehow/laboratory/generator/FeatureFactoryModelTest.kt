package io.mehow.laboratory.generator

import com.squareup.kotlinpoet.ClassName
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mehow.laboratory.generator.Visibility.Internal
import io.mehow.laboratory.generator.Visibility.Public
import io.mehow.laboratory.generator.test.shouldSpecify
import kotlin.DeprecationLevel.WARNING
import org.junit.Test

class FeatureFactoryModelTest {
  val featureA =
    FeatureFlagModel(
      ClassName("io.mehow", "FeatureA"),
      listOf(FeatureFlagOption("A", isDefault = true)),
    )

  val featureB =
    FeatureFlagModel(
      ClassName("io.mehow", "FeatureB"),
      listOf(FeatureFlagOption("A", isDefault = true)),
    )

  val featureC =
    FeatureFlagModel(
      ClassName("io.mehow.c", "FeatureA"),
      listOf(FeatureFlagOption("A", isDefault = true)),
    )

  @Test
  fun `internal visibility`() {
    val model =
      FeatureFactoryModel(
        ClassName("io.mehow", "Factory"),
        listOf(featureA, featureB, featureC),
        visibility = Internal,
      )

    val fileSpec = model.prepare()

    fileSpec shouldSpecify
      """
        package io.mehow

        import io.mehow.laboratory.Feature
        import io.mehow.laboratory.FeatureFactory
        import io.mehow.laboratory.`internal`.InternalLaboratoryApi
        import kotlin.Suppress
        import kotlin.collections.List
        import kotlin.collections.Set
        import kotlin.collections.setOf
        import kotlin.reflect.KClass

        internal fun FeatureFactory.Companion.generated(): FeatureFactory = Factory

        private object Factory : FeatureFactory {
          @Suppress("UNCHECKED_CAST")
          override fun create(): Set<KClass<out Feature<*>>> = setOf(
            FeatureA::class,
            FeatureB::class,
            io.mehow.c.FeatureA::class
          ) as Set<KClass<out Feature<*>>>

          @InternalLaboratoryApi
          override fun options(feature: KClass<out Feature<*>>): List<Feature<*>>? = when (feature) {
            FeatureA::class -> FeatureA.entries
            FeatureB::class -> FeatureB.entries
            io.mehow.c.FeatureA::class -> io.mehow.c.FeatureA.entries
            else -> null
          }
        }
        """
  }

  @Test
  fun `public visibility`() {
    val model =
      FeatureFactoryModel(
        ClassName("io.mehow", "Factory"),
        listOf(featureA, featureB, featureC),
        visibility = Public,
      )

    val fileSpec = model.prepare()

    fileSpec shouldSpecify
      """
        package io.mehow

        import io.mehow.laboratory.Feature
        import io.mehow.laboratory.FeatureFactory
        import io.mehow.laboratory.`internal`.InternalLaboratoryApi
        import kotlin.Suppress
        import kotlin.collections.List
        import kotlin.collections.Set
        import kotlin.collections.setOf
        import kotlin.reflect.KClass

        public fun FeatureFactory.Companion.generated(): FeatureFactory = Factory

        private object Factory : FeatureFactory {
          @Suppress("UNCHECKED_CAST")
          override fun create(): Set<KClass<out Feature<*>>> = setOf(
            FeatureA::class,
            FeatureB::class,
            io.mehow.c.FeatureA::class
          ) as Set<KClass<out Feature<*>>>

          @InternalLaboratoryApi
          override fun options(feature: KClass<out Feature<*>>): List<Feature<*>>? = when (feature) {
            FeatureA::class -> FeatureA.entries
            FeatureB::class -> FeatureB.entries
            io.mehow.c.FeatureA::class -> io.mehow.c.FeatureA.entries
            else -> null
          }
        }
        """
  }

  @Test
  fun `no features`() {
    val model = FeatureFactoryModel(ClassName("io.mehow", "Factory"), features = emptyList())

    val fileSpec = model.prepare()

    fileSpec shouldSpecify
      """
        package io.mehow

        import io.mehow.laboratory.Feature
        import io.mehow.laboratory.FeatureFactory
        import io.mehow.laboratory.`internal`.InternalLaboratoryApi
        import kotlin.collections.List
        import kotlin.collections.Set
        import kotlin.collections.emptySet
        import kotlin.reflect.KClass

        internal fun FeatureFactory.Companion.generated(): FeatureFactory = Factory

        private object Factory : FeatureFactory {
          override fun create(): Set<KClass<out Feature<*>>> = emptySet<KClass<out Feature<*>>>()

          @InternalLaboratoryApi
          override fun options(feature: KClass<out Feature<*>>): List<Feature<*>>? = null
        }
        """
  }

  @Test
  fun `file-level deprecation suppress when any feature is deprecated`() {
    val deprecatedFeature =
      FeatureFlagModel(
        ClassName("io.mehow", "DeprecatedFeature"),
        listOf(FeatureFlagOption("A", isDefault = true)),
        deprecation = Deprecation("Sample deprecation", WARNING),
      )
    val model =
      FeatureFactoryModel(
        ClassName("io.mehow", "Factory"),
        listOf(featureA, deprecatedFeature),
      )

    val code = model.prepare().toString()

    code shouldContain "@file:Suppress(\"DEPRECATION\")"
    code shouldContain "DeprecatedFeature::class"
  }

  @Test
  fun `no file-level deprecation suppress when no feature is deprecated`() {
    val model =
      FeatureFactoryModel(
        ClassName("io.mehow", "Factory"),
        listOf(featureA, featureB),
      )

    val code = model.prepare().toString()

    code shouldNotContain "@file:Suppress"
  }
}
