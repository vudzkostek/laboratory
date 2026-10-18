package io.mehow.laboratory.generator

import com.squareup.kotlinpoet.ClassName
import io.mehow.laboratory.generator.Visibility.Internal
import io.mehow.laboratory.generator.Visibility.Public
import io.mehow.laboratory.generator.test.shouldSpecify
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
        import kotlin.Suppress
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
        import kotlin.Suppress
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
        import kotlin.collections.Set
        import kotlin.collections.emptySet
        import kotlin.reflect.KClass

        internal fun FeatureFactory.Companion.generated(): FeatureFactory = Factory

        private object Factory : FeatureFactory {
          override fun create(): Set<KClass<out Feature<*>>> = emptySet<KClass<out Feature<*>>>()
        }
        """
  }
}
