# Laboratory ⚗️

[![Maven Central](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fio%2Fmehow%2Flaboratory%2Flaboratory%2Fmaven-metadata.xml&style=flat-square&label=maven-central&color=%235C96B2)](https://central.sonatype.com/namespace/io.mehow.laboratory)
[![Snapshots](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fio%2Fmehow%2Flaboratory%2Flaboratory%2Fmaven-metadata.xml&style=flat-square&label=snapshots&color=%2315252D&strategy=latestProperty)](https://central.sonatype.com/repository/maven-snapshots/io/mehow/laboratory/)

Feature flags for multi-module Kotlin Android projects.

Please visit [project website](https://mehow.io/laboratory/) for the full documentation and the [changelog](https://mehow.io/laboratory/changelog/).

## TLDR

Starting from `3.0.0`, Laboratory is a Kotlin Multiplatform library. The same SDK
powers Android and iOS consumers through a single `io.mehow.laboratory:laboratory`
artifact (with per-target variants published automatically for Android, JVM, and the
three iOS architectures). The debug inspector screen is a Compose Multiplatform module
you can plug into either platform.

### Android

Add Laboratory as a dependency.

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation "io.mehow.laboratory:laboratory:3.0.0"
}
```

Define your feature flags.

```kotlin
enum class AuthType : Feature<AuthType> {
  None,
  Fingerprint,
  Retina,
  Face;

  public override val defaultOption get() = Fingerprint
}
```

Start using them in the application.

```kotlin
suspend fun main() {
  // A high-level API for interaction with feature flags
  val laboratory = Laboratory.inMemory()

  // Set AuthType option to Fingerprint
  val success = laboratory.setOption(AuthType.Fingerprint)

  // Check what is the current option of AuthType
  val currentAuthType = laboratory.experiment<AuthType>()

  // Check if the current option of AuthType is equal to Face
  val isFaceAuth = laboratory.experimentIs(AuthType.Face)

  // Observe changes to the AuthType feature flag
  laboratory.observe<AuthType>()
      .onEach { option -> println("AuthType: $option") }
      .launchIn(GlobalScope)
}
```

### iOS (CocoaPods)

Add the generated pods to your `Podfile` — the SDK ships them from the KMP module's
`podPublishXCFramework` task.

```ruby
pod 'Laboratory'            # the SDK
pod 'LaboratoryInspector'   # the Compose Multiplatform debug inspector (debug-only pod)
```

Then from Swift:

```swift
import Laboratory
import LaboratoryInspector

let storage = StorageCompanion.shared.dataStore(name: "laboratory")
let laboratory = LaboratoryCompanion.shared.builder()
    .localStorage(storage: storage)
    .featureFactory(factory: GeneratedFeatureFactory_shared.generated())
    .build()

let configuration = LaboratoryConfiguration.Companion.shared.create(
    laboratory: laboratory,
    featureFactory: GeneratedFeatureFactory_shared.generated(),
    sectionName: "Features",
    deprecationHandler: DeprecationHandler.Companion.shared.Default
)
present(LaboratoryViewControllerKt.LaboratoryViewController(configuration: configuration))
```

Reify-based lookups (`experiment<T>()`, `observe<T>()`) only exist on Kotlin's reified
APIs. On iOS, wire a generated `FeatureFactory` into the builder so the inspector and
non-reified call sites can resolve enum entries without JVM reflection.

## License

    Copyright 2020 Michał Sikora

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
