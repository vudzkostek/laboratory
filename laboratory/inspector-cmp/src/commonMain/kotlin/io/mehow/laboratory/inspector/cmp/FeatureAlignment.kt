package io.mehow.laboratory.inspector.cmp

/** Specifies how deprecated feature flags are positioned within a list of features. */
public enum class FeatureAlignment {
  /** No special ordering. */
  Regular,

  /** Deprecated feature flags are moved to the bottom of the list. */
  Bottom;

  /** Interface to choose a [FeatureAlignment] for a feature based on its [DeprecationLevel]. */
  public fun interface Selector {
    /** Returns the appropriate [FeatureAlignment] for the given [level]. */
    public fun select(level: DeprecationLevel): FeatureAlignment
  }
}
