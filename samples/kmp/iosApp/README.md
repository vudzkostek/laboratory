# Laboratory KMP sample — iOS app

This directory is a minimal scaffold showing how to consume the Laboratory SDK + the
Compose Multiplatform inspector from an iOS app via CocoaPods. It is intentionally not a
complete, buildable Xcode project — scaffolding Xcode projects outside of Xcode is awkward
and brittle. Use the artifacts here as a starting point and finish wiring the project
inside Xcode.

## What you need

1. Xcode 15 or newer.
2. CocoaPods installed (`sudo gem install cocoapods`).
3. The two local XCFrameworks produced by the SDK + inspector modules. From the repo root:

   ```sh
   ./gradlew \
     :laboratory:shared:podPublishXCFramework \
     :laboratory:inspector-cmp:podPublishXCFramework \
     --no-configuration-cache
   ```

   This writes:
   - `laboratory/shared/build/cocoapods/publish/{debug,release}/Laboratory.xcframework`
   - `laboratory/inspector-cmp/build/cocoapods/publish/{debug,release}/LaboratoryInspector.xcframework`
   - Plus local `.podspec` files next to each XCFramework.

4. The KMP sample module's framework. From the `samples/` directory:

   ```sh
   ./gradlew :kmp:podPublishXCFramework --no-configuration-cache
   ```

   This writes:
   - `samples/kmp/build/cocoapods/publish/{debug,release}/LaboratorySample.xcframework`
   - `samples/kmp/build/cocoapods/publish/{debug,release}/kmp.podspec`

## Wiring the Xcode project

1. In Xcode, `File > New > Project > iOS App` (SwiftUI, Swift, Deployment Target 14.0+).
   Save it inside this directory (`samples/kmp/iosApp/iosApp.xcodeproj`).
2. Close the project and drop the included `Podfile` next to the `.xcodeproj`.
3. From this directory, run `pod install`.
4. Open `iosApp.xcworkspace` (the Pods-generated workspace).
5. Replace the SwiftUI content view with the contents of `ContentView.swift` from this
   directory. The important call is:

   ```swift
   let vc = InspectorHostKt.debugInspectorViewController()
   ```

   `InspectorHost` is Kotlin's source file name — Kotlin top-level functions compile to
   `<FileName>Kt`, so Swift sees `InspectorHostKt`.
6. Build + run on an Arm simulator (iPhone 15 or newer).

## Known limitations

- The `Podfile` points at the local `publish/debug/` directories. In a real downstream
  app you would publish the pods to a private spec repo or vendor the XCFrameworks.
- `LaboratorySample.xcframework` directly re-exports `Laboratory` and
  `LaboratoryInspector` because KMP's CocoaPods plugin links them into one static
  framework. If you want to see the inspector pod as a separate dependency, remove the
  `pod("Laboratory") { source = path(...) }` / `pod("LaboratoryInspector") { source =
  path(...) }` blocks from `samples/kmp/build.gradle.kts` once your CI publishes real
  podspecs.
