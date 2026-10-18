package io.mehow.laboratory.sample.kmp

import io.mehow.laboratory.Laboratory
import io.mehow.laboratory.Storage
import io.mehow.laboratory.datastore.dataStore
import io.mehow.laboratory.inspector.cmp.LaboratoryConfiguration
import io.mehow.laboratory.inspector.cmp.LaboratoryViewController
import platform.UIKit.UIViewController

/**
 * One-call entry point for the iOS sample: builds the laboratory + inspector configuration and
 * returns a presentable `UIViewController`. Call from Swift like:
 *
 * ```swift
 * import LaboratorySample
 *
 * let vc = InspectorHostKt.debugInspectorViewController()
 * present(vc, animated: true)
 * ```
 */
@Suppress("FunctionName")
public fun debugInspectorViewController(): UIViewController {
  val laboratory = createLaboratory(Storage.dataStore())
  val configuration = createInspectorConfiguration(laboratory)
  return LaboratoryViewController(configuration)
}

/** Builds the sample [Laboratory] using DataStore-backed [Storage]. */
public fun buildSampleLaboratory(): Laboratory = createLaboratory(Storage.dataStore())

/** Builds the inspector configuration for a given [Laboratory] instance. */
public fun buildSampleInspectorConfiguration(laboratory: Laboratory): LaboratoryConfiguration =
  createInspectorConfiguration(laboratory)
