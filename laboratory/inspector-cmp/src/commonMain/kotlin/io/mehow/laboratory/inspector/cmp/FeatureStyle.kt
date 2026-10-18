package io.mehow.laboratory.inspector.cmp

/** Specifies how deprecated feature flags are displayed in the Compose inspector UI. */
public enum class FeatureStyle {
  /** Show deprecated feature flags with no special styling. */
  Show,

  /** Display the feature flag's name with a strikethrough. */
  Strikethrough,

  /** Remove the feature flag from the list entirely. */
  Hide;

  /** Interface to select a [FeatureStyle] based on a feature's [DeprecationLevel]. */
  public fun interface Selector {
    /** Returns the appropriate [FeatureStyle] for the given [level]. */
    public fun select(level: DeprecationLevel): FeatureStyle
  }
}
