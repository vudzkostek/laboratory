package io.mehow.laboratory

internal enum class FeatureA : Feature<FeatureA> {
  A,
  B,
  C;

  override val defaultOption
    get() = A
}

internal enum class FeatureB : Feature<FeatureB> {
  A,
  B,
  C;

  override val defaultOption
    get() = B
}

internal enum class FeatureC : Feature<FeatureC> {
  A,
  B,
  C;

  override val defaultOption
    get() = C
}

internal enum class RemoteFeatureA : Feature<RemoteFeatureA> {
  A,
  B,
  C;

  override val defaultOption
    get() = A

  override val defaultSource
    get() = Feature.Source.Remote
}

internal enum class RemoteFeatureB : Feature<RemoteFeatureB> {
  A,
  B,
  C;

  override val defaultOption
    get() = B

  override val defaultSource
    get() = Feature.Source.Remote
}

internal enum class FeatureWithoutValues : Feature<FeatureWithoutValues>

internal enum class EnabledBinaryFeature(override val binaryValue: Boolean) :
  BinaryFeature<EnabledBinaryFeature> {
  Enabled(binaryValue = true),
  Disabled(binaryValue = false);

  override val defaultOption
    get() = Enabled
}

internal enum class DisabledBinaryFeature(override val binaryValue: Boolean) :
  BinaryFeature<DisabledBinaryFeature> {
  Enabled(binaryValue = true),
  Disabled(binaryValue = false);

  override val defaultOption
    get() = Disabled
}
