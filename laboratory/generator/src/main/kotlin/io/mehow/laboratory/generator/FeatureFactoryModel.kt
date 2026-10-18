package io.mehow.laboratory.generator

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.PRIVATE
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.joinToCode
import io.mehow.laboratory.Feature
import io.mehow.laboratory.FeatureFactory
import io.mehow.laboratory.generator.Visibility.Internal
import io.mehow.laboratory.internal.InternalLaboratoryApi
import kotlin.reflect.KClass

public class FeatureFactoryModel(
  public val className: ClassName,
  public val features: List<FeatureFlagModel>,
  public val visibility: Visibility = Internal,
) {
  public fun prepare(): FileSpec = FeatureFactoryGenerator(this).fileSpec()
}

private class FeatureFactoryGenerator(factory: FeatureFactoryModel) {
  private val sortedFeatures = factory.features.sortedBy { it.className.reflectionName() }

  private val featureClasses =
    sortedFeatures
      .map { feature -> CodeBlock.of("%T::class", feature.className) }
      .joinToCode(prefix = "\n⇥", separator = ",\n", suffix = "⇤\n")

  private val suppressCast =
    AnnotationSpec.builder(Suppress::class).addMember("%S", "UNCHECKED_CAST").build()

  private val internalApi = AnnotationSpec.builder(InternalLaboratoryApi::class).build()

  private val setOf = MemberName("kotlin.collections", "setOf")

  private val emptySet = MemberName("kotlin.collections", "emptySet")

  private val discoveryFunctionOverride =
    FunSpec.builder("create")
      .addModifiers(OVERRIDE)
      .apply {
        returns(factoryReturnType)

        if (factory.features.isNotEmpty()) {
          addAnnotation(suppressCast)
          addStatement("return %M(%L) as %T", setOf, featureClasses, factoryReturnType)
        } else {
          addStatement("return %M<%T>()", emptySet, featureType)
        }
      }
      .build()

  private val featureParameter = ParameterSpec.builder("feature", featureType).build()

  private val optionsBranches =
    sortedFeatures
      .map { feature ->
        CodeBlock.of("%T::class·->·%T.entries", feature.className, feature.className)
      }
      .joinToCode(prefix = "when·(feature)·{\n⇥", separator = "\n", suffix = "\nelse·->·null⇤\n}")

  private val optionsFunctionOverride =
    FunSpec.builder("options")
      .addModifiers(OVERRIDE)
      .addAnnotation(internalApi)
      .addParameter(featureParameter)
      .returns(optionsReturnType)
      .apply {
        if (sortedFeatures.isEmpty()) {
          addStatement("return null")
        } else {
          addStatement("return %L", optionsBranches)
        }
      }
      .build()

  private val factoryType =
    TypeSpec.objectBuilder(factory.className)
      .addModifiers(PRIVATE)
      .addSuperinterface(FeatureFactory::class)
      .addFunction(discoveryFunctionOverride)
      .addFunction(optionsFunctionOverride)
      .build()

  private val factoryExtension =
    FunSpec.builder("generated")
      .addModifiers(factory.visibility.modifier)
      .receiver(FeatureFactory.Companion::class)
      .returns(FeatureFactory::class)
      .addStatement("return %N", factoryType)
      .build()

  private val suppressDeprecationFileAnnotation: AnnotationSpec? =
    if (factory.features.any { it.deprecation != null }) {
      AnnotationSpec.builder(Suppress::class)
        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
        .addMember("%S", "DEPRECATION")
        .build()
    } else {
      null
    }

  private val factoryFile =
    FileSpec.builder(factory.className.packageName, factory.className.simpleName)
      .apply { suppressDeprecationFileAnnotation?.let(::addAnnotation) }
      .addFunction(factoryExtension)
      .addType(factoryType)
      .build()

  fun fileSpec() = factoryFile

  private companion object {
    val featureType = KClass::class(WildcardTypeName.producerOf(Feature::class(STAR)))
    val factoryReturnType = Set::class(featureType)
    val optionsReturnType = List::class(Feature::class(STAR)).copy(nullable = true)
  }
}
