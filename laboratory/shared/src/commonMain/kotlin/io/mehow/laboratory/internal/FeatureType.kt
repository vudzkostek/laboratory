package io.mehow.laboratory.internal

import io.mehow.laboratory.Feature
import io.mehow.laboratory.FeatureFactory
import kotlin.reflect.KClass

/**
 * Looks up the list of options for a feature flag [KClass] via runtime reflection.
 *
 * Platform-specific bridge: implemented via JVM reflection on the JVM and Android, and left
 * unsupported on iOS. On iOS, callers must consult a generator-emitted [FeatureFactory] registry
 * instead — register one via `Laboratory.Builder.featureFactory(...)`.
 */
@InternalLaboratoryApi public expect fun KClass<out Feature<*>>.optionsRaw(): List<Feature<*>>

@InternalLaboratoryApi
@Suppress("UNCHECKED_CAST")
public fun <T> KClass<out T>.options(): List<T> where T : Feature<T>, T : Enum<out T> =
  (this as KClass<out Feature<*>>).optionsRaw() as List<T>

@InternalLaboratoryApi
public fun <T> KClass<out T>.firstOption(): T where T : Feature<T>, T : Enum<out T> =
  options().first()

@InternalLaboratoryApi
public fun <T> KClass<out T>.defaultOption(): T where T : Feature<T>, T : Enum<out T> =
  firstOption().defaultOption

@InternalLaboratoryApi
public fun <T> KClass<out T>.defaultSource(): Feature.Source where T : Feature<T>, T : Enum<out T> =
  firstOption().defaultSource

@InternalLaboratoryApi
public fun KClass<out Feature<*>>.firstOptionRaw(): Feature<*> = optionsRaw().first()

@InternalLaboratoryApi
public fun KClass<out Feature<*>>.defaultOptionRaw(): Feature<*> = firstOptionRaw().defaultOption

@InternalLaboratoryApi
public fun KClass<out Feature<*>>.defaultSourceRaw(): Feature.Source =
  firstOptionRaw().defaultSource

/** Overloads that prefer a generator-emitted [FeatureFactory] registry over runtime reflection. */
@InternalLaboratoryApi
public fun KClass<out Feature<*>>.firstOptionRaw(featureFactory: FeatureFactory?): Feature<*> =
  (featureFactory?.options(this) ?: optionsRaw()).first()

@InternalLaboratoryApi
public fun KClass<out Feature<*>>.defaultOptionRaw(featureFactory: FeatureFactory?): Feature<*> =
  firstOptionRaw(featureFactory).defaultOption

@InternalLaboratoryApi
public fun KClass<out Feature<*>>.defaultSourceRaw(
  featureFactory: FeatureFactory?
): Feature.Source = firstOptionRaw(featureFactory).defaultSource
