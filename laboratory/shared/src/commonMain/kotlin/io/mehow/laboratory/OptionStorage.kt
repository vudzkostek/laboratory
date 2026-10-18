package io.mehow.laboratory

import io.mehow.laboratory.internal.InternalLaboratoryApi
import io.mehow.laboratory.internal.options
import io.mehow.laboratory.internal.optionsRaw
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists and retrieves feature options using a key-value [Storage]. Options are stored by name
 * and resolved via feature enum.
 */
public open class OptionStorage internal constructor(protected val storage: Storage) {
  /** Gets the selected option for the feature or `null` if unset. */
  public suspend fun <T> getOption(feature: KClass<out T>): T?
    where T : Feature<T>, T : Enum<out T> =
    storage.getString(feature.storageKey)?.let(feature::findOption)

  /** Observes changes to the feature's selected option. */
  public fun <T> observeOption(feature: KClass<out T>): Flow<T?>
    where T : Feature<T>, T : Enum<out T> =
    storage.stringFlow(feature.storageKey).map { value -> value?.let(feature::findOption) }

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
}

/** Extension of [OptionStorage] that also manages feature source selection. */
public class SourceOptionStorage internal constructor(storage: Storage) : OptionStorage(storage) {
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

internal class RawOptionStorage(private val storage: Storage) {
  fun observeOption(feature: KClass<out Feature<*>>): Flow<Feature<*>?> =
    storage.stringFlow(feature.storageKey).map { value -> value?.let(feature::findOption) }

  fun observeSource(feature: KClass<out Feature<*>>): Flow<Feature.Source?> =
    storage.booleanFlow(feature.storageKey).map { value -> value?.let(Boolean::toFeatureSource) }
}

@OptIn(InternalLaboratoryApi::class)
private fun <T> KClass<out T>.findOption(name: String): T? where T : Feature<T>, T : Enum<out T> =
  options().firstOrNull { option ->
    option.name == name
  }

@OptIn(InternalLaboratoryApi::class)
private fun KClass<out Feature<*>>.findOption(name: String): Feature<*>? =
  optionsRaw().firstOrNull { option ->
    option.name == name
  }

/**
 * Storage key used for the feature flag identified by this [KClass].
 *
 * Defaults to the class's [qualifiedName] (which on the JVM matches the previous `Class.name`-based
 * key byte-for-byte for top-level feature enums) and falls back to [simpleName] for anonymous/local
 * classes where no qualified name is available.
 *
 * TODO Phase 4: replace with generator-emitted `storageKey` constants on each feature enum.
 */
internal val KClass<out Feature<*>>.storageKey: String
  get() = qualifiedName ?: simpleName!!

private fun Boolean.toFeatureSource() = if (this) Feature.Source.Remote else Feature.Source.Local
