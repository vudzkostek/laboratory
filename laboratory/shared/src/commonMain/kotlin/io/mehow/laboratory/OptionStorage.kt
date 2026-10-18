package io.mehow.laboratory

import io.mehow.laboratory.internal.InternalLaboratoryApi
import io.mehow.laboratory.internal.optionsRaw
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists and retrieves feature options using a key-value [Storage]. Options are stored by name
 * and resolved via feature enum.
 */
public open class OptionStorage
internal constructor(protected val storage: Storage, internal val featureFactory: FeatureFactory?) {
  /** Gets the selected option for the feature or `null` if unset. */
  public suspend fun <T> getOption(feature: KClass<out T>): T?
    where T : Feature<T>, T : Enum<out T> =
    storage.getString(feature.storageKey)?.let { name -> findOption(feature, name) }

  /** Observes changes to the feature's selected option. */
  public fun <T> observeOption(feature: KClass<out T>): Flow<T?>
    where T : Feature<T>, T : Enum<out T> =
    storage.stringFlow(feature.storageKey).map { value ->
      value?.let { name -> findOption(feature, name) }
    }

  /** Stores a feature options. */
  public suspend fun setOption(option: Feature<*>): Boolean =
    storage.setString(option.storageKey, option.name)

  /** Stores one or more feature options. */
  public suspend fun setOptions(option: Feature<*>, vararg options: Feature<*>): Boolean {
    val entries = buildMap {
      put(option.storageKey, option.name)
      for (option in options) {
        put(option.storageKey, option.name)
      }
    }
    return storage.setStrings(entries)
  }

  /** Stores a collection of feature options. */
  public suspend fun setOptions(options: Collection<Feature<*>>): Boolean {
    val entries = options.associate { option -> option.storageKey to option.name }
    return storage.setStrings(entries)
  }

  /** Clears all stored feature options. */
  public suspend fun clear(): Boolean = storage.clear()

  @Suppress("UNCHECKED_CAST")
  private fun <T> findOption(feature: KClass<out T>, name: String): T?
    where T : Feature<T>, T : Enum<out T> =
    resolveOptions(featureFactory, feature as KClass<out Feature<*>>).firstOrNull { option ->
      option.name == name
    } as T?
}

/** Extension of [OptionStorage] that also manages feature source selection. */
public class SourceOptionStorage
internal constructor(storage: Storage, featureFactory: FeatureFactory?) :
  OptionStorage(storage, featureFactory) {
  /** Gets the selected source for the feature or `null` if unset. */
  public suspend fun <T> getSource(feature: KClass<out T>): Feature.Source?
    where T : Feature<T>, T : Enum<out T> =
    storage.getBoolean(feature.storageKey)?.let(Boolean::toFeatureSource)

  /** Observes changes to the feature's source. */
  public fun <T> observeSource(feature: KClass<out T>): Flow<Feature.Source?>
    where T : Feature<T>, T : Enum<out T> =
    storage.booleanFlow(feature.storageKey).map { value -> value?.let(Boolean::toFeatureSource) }

  /** Sets the feature source to local. */
  public suspend inline fun <reified T> setLocalSource(): Boolean
    where T : Feature<T>, T : Enum<out T> = setLocalSource(T::class)

  /** Sets the feature source to local. */
  public suspend fun setLocalSource(feature: KClass<out Feature<*>>): Boolean =
    storage.setBoolean(feature.storageKey, false)

  /** Sets the feature source to remote. */
  public suspend inline fun <reified T> setRemoteSource(): Boolean
    where T : Feature<T>, T : Enum<out T> = setRemoteSource(T::class)

  /** Sets the feature source to remote. */
  public suspend fun setRemoteSource(feature: KClass<out Feature<*>>): Boolean =
    storage.setBoolean(feature.storageKey, true)
}

internal class RawOptionStorage(
  private val storage: Storage,
  private val featureFactory: FeatureFactory?,
) {
  fun observeOption(feature: KClass<out Feature<*>>): Flow<Feature<*>?> =
    storage.stringFlow(feature.storageKey).map { value ->
      value?.let { name ->
        resolveOptions(featureFactory, feature).firstOrNull { option -> option.name == name }
      }
    }

  fun observeSource(feature: KClass<out Feature<*>>): Flow<Feature.Source?> =
    storage.booleanFlow(feature.storageKey).map { value -> value?.let(Boolean::toFeatureSource) }
}

/**
 * Resolves the options for a feature by first consulting the generator-emitted registry on
 * [featureFactory] and falling back to runtime reflection on platforms that support it (JVM,
 * Android).
 *
 * On platforms without reflection (iOS), the fallback throws, so callers must register a generated
 * [FeatureFactory] via [Laboratory.Builder.featureFactory] to make non-reified lookups work.
 */
@OptIn(InternalLaboratoryApi::class)
internal fun resolveOptions(
  featureFactory: FeatureFactory?,
  feature: KClass<out Feature<*>>,
): List<Feature<*>> = featureFactory?.options(feature) ?: feature.optionsRaw()

/**
 * Storage key used for the feature flag identified by this [KClass].
 *
 * Defaults to the class's [qualifiedName] (which on the JVM matches the previous `Class.name`-based
 * key byte-for-byte for top-level feature enums) and falls back to [simpleName] for anonymous/local
 * classes where no qualified name is available. Generator-emitted enums override
 * [Feature.storageKey] directly with the FQCN captured at generation time; this extension is used
 * by the storage layer to look up boolean (source) and string (option) entries for a given
 * [KClass].
 */
internal val KClass<out Feature<*>>.storageKey: String
  get() = qualifiedName ?: simpleName!!

private fun Boolean.toFeatureSource() = if (this) Feature.Source.Remote else Feature.Source.Local
