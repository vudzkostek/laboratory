package io.mehow.laboratory.inspector.cmp

/**
 * Decides how deprecated feature flags are displayed in the Compose inspector.
 *
 * The CMP inspector consults the configured [FeatureStyle.Selector] and [FeatureAlignment.Selector]
 * to answer "how should this feature look?" and "where should it appear in the list?" for each
 * deprecation level. Phase 5 wires both selectors but applies them minimally: deprecated features
 * can be hidden and ordered to the bottom, while visual strikethrough on names is left as a Phase 6
 * polish target so this phase stays focused on the core module.
 */
public class DeprecationHandler
internal constructor(
  internal val styleSelector: FeatureStyle.Selector,
  internal val alignmentSelector: FeatureAlignment.Selector,
) {
  internal fun style(level: DeprecationLevel): FeatureStyle = styleSelector.select(level)

  internal fun alignment(level: DeprecationLevel): FeatureAlignment =
    alignmentSelector.select(level)

  /** Factory and bundled defaults for [DeprecationHandler]. */
  public companion object {
    /**
     * Default handler: deprecated features are shown with a strikethrough and sent to the bottom.
     */
    public val Default: DeprecationHandler =
      DeprecationHandler(
        styleSelector = FeatureStyle.Selector { FeatureStyle.Strikethrough },
        alignmentSelector = FeatureAlignment.Selector { FeatureAlignment.Bottom },
      )

    /** Creates a custom [DeprecationHandler]. */
    public fun create(
      styleSelector: FeatureStyle.Selector,
      alignmentSelector: FeatureAlignment.Selector,
    ): DeprecationHandler = DeprecationHandler(styleSelector, alignmentSelector)
  }
}
