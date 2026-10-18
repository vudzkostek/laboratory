package io.mehow.laboratory.inspector.cmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Wraps [LaboratoryInspector] in a `UIViewController` that Swift callers can present.
 *
 * Typical usage from Swift:
 * ```swift
 * let configuration = LaboratoryConfiguration(
 *   laboratory: appLaboratory,
 *   featureFactories: ["Features": GeneratedFeatureFactory_shared.generated()]
 * )
 * let vc = LaboratoryInspectorKt.LaboratoryViewController(configuration: configuration)
 * present(vc, animated: true)
 * ```
 */
@Suppress("FunctionName")
public fun LaboratoryViewController(configuration: LaboratoryConfiguration): UIViewController =
  ComposeUIViewController {
    LaboratoryInspector(configuration)
  }
