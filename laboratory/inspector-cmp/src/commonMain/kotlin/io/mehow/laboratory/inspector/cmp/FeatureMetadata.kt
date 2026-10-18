package io.mehow.laboratory.inspector.cmp

import io.mehow.laboratory.Feature
import io.mehow.laboratory.FeatureFactory
import kotlin.reflect.KClass

/**
 * Row model for the inspector's feature list.
 *
 * Resolved from the generator-emitted [FeatureFactory] registry only — this inspector never calls
 * JVM reflection, so the Android target and iOS target share identical data flow.
 */
internal data class FeatureMetadata(
  val type: KClass<out Feature<*>>,
  val name: String,
  val description: String,
  val options: List<Feature<*>>,
  val defaultOption: Feature<*>,
  val defaultSource: Feature.Source,
  val style: FeatureStyle,
  val alignment: FeatureAlignment,
) {
  val nameQuery: QueryString = QueryString.create(name)
  private val optionQueries: List<QueryString> = options.map { QueryString.create(it.name) }

  fun matches(query: QueryString): Boolean =
    query.isEmpty() ||
      query in nameQuery ||
      optionQueries.any { optionQuery -> query in optionQuery }
}

internal fun loadMetadata(
  featureFactory: FeatureFactory,
  deprecationHandler: DeprecationHandler,
): List<FeatureMetadata> {
  val features = featureFactory.create()
  return features
    .mapNotNull { type ->
      val options = featureFactory.resolveOptions(type) ?: return@mapNotNull null
      if (options.isEmpty()) return@mapNotNull null
      val level = deprecationLevel(type)
      val style = level?.let(deprecationHandler::style) ?: FeatureStyle.Show
      if (style == FeatureStyle.Hide) return@mapNotNull null
      val qualifiedName = type.qualifiedName ?: type.simpleName.orEmpty()
      val displayName = qualifiedName.substringAfterLast('.').replace('$', '.').ifEmpty { "?" }
      FeatureMetadata(
        type = type,
        name = displayName,
        description = options.first().description,
        options = options,
        defaultOption = options.first().defaultOption,
        defaultSource = options.first().defaultSource,
        style = style,
        alignment = level?.let(deprecationHandler::alignment) ?: FeatureAlignment.Regular,
      )
    }
    .sortedWith(compareBy({ it.alignment }, { it.name }))
}

/**
 * Fetches options for the feature from the generator-emitted registry. Falls back to [emptyList]
 * when the factory doesn't know the feature so the UI can skip rendering a row rather than
 * crashing.
 */
@OptIn(io.mehow.laboratory.internal.InternalLaboratoryApi::class)
private fun FeatureFactory.resolveOptions(type: KClass<out Feature<*>>): List<Feature<*>>? =
  options(type)

/**
 * Returns the deprecation level of a feature flag type when one is applied.
 *
 * Phase 5 ships a platform-agnostic stub — the Android `:inspector` reads `@Deprecated` annotations
 * through Java reflection, which is unavailable on iOS. Wiring a cross-platform deprecation lookup
 * through the generator is a Phase 6 polish target (`TODO Phase 6`).
 */
@Suppress("UnusedParameter") // Phase 6 will wire this through the generator.
internal expect fun deprecationLevel(type: KClass<out Feature<*>>): DeprecationLevel?
