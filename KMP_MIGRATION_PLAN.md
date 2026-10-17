# Laboratory → Kotlin Multiplatform Migration Plan

> **Status:** draft after Phase 0 analysis. No library code has been changed.
> **Scope:** rewrite `laboratory` core so Android + iOS consumers get the same SDK (storage + generator), keep the Hyperion plugin Android-only, add a simple shared debug screen that iOS can present with one call.
> **Non-goals:** no logic changes, no new storage backends beyond DataStore, no new public features. The Hyperion module keeps its current UX for Android.

---

## 0. TL;DR

- **Effort:** medium. With the user's confirmed decisions (one SDK, DataStore-only, CocoaPods, keep Android `:inspector` as-is, add a CMP inspector mainly for iOS), the realistic size is **4 focused agent sessions (~1 day each)** for everything except the inspector UI, plus **~1.5 days** for the Compose Multiplatform inspector.
- **iOS debug screen:** **Compose Multiplatform** inspector in a new `:inspector-cmp` module, exposed on iOS as `fun LaboratoryViewController(configuration): UIViewController` via `ComposeUIViewController`. One method call from Swift. Android **keeps the existing Views-based `:inspector` + `:hyperion-plugin` as-is** (only the `KClass` API migration touches them). `:inspector-cmp` is also published for Android for consumers who skip Hyperion — Android then has two possible debug screens, which is accepted.
- **Top risks:**
  1. `Class<T>`-based public API in `runtime` and `inspector` (every `experiment`, `observe`, `OptionStorage`, `FeatureFactory`, `OptionFactory` takes `Class<out Feature<*>>`). Must migrate to `KClass<out Feature<*>>`. **Breaking change**, affects every consumer, including samples and the Hyperion plugin.
  2. Enum reflection (`Class.enumConstants`, cached in `internal/FeatureType.kt`) is JVM-only. Replace with `kotlin.enums.enumEntries` + stable name/ordinal lookup; keep thread-safety with `kotlinx.atomicfu` instead of `java.util.concurrent`.
  3. `runBlocking` in `BlockingLaboratory` is unavailable on K/N. Either drop blocking API from common, or keep it JVM-only via `expect/actual`.
  4. Wire-generated `StorageData` is `java.io.InputStream/OutputStream`-based. `androidx.datastore-core` is Android/JVM only; use `androidx.datastore-core-okio` + `okio.BufferedSource/Sink` for KMP, or write a thin protobuf-free `Storage` using JSON. Preferred: Okio DataStore + Wire's `ProtoAdapter` on `okio.BufferedSource`.
  5. Gradle plugin currently **errors** on `org.jetbrains.kotlin.multiplatform` and binds generated sources to a Kotlin JVM source set or AGP variant. It needs to detect KMP, add sources to `commonMain` (or the appropriate target), be configuration-cache clean, and remain backwards compatible with pure Kotlin/JVM and pure AGP consumers.
  6. SharedPreferences → DataStore migration: existing Android users already have state keyed `string:<name>` / `boolean:<name>`. Ship a one-shot migration (`SharedPreferencesMigration` from AndroidX or custom) so they do not lose selected flags.
  7. `::class.java.canonicalName` is used as the storage key for a feature (`OptionStorage.storageKey`). On K/N there is no stable `canonicalName`. Pick an explicit, portable key — the generated feature's fully qualified class name is already known at code-gen time, so store it in a generated `storageKey` property on `Feature` or in a companion.

- **Phases (each sized for one agent session):**
  P1. Workspace plumbing + `:shared` KMP module scaffold (Android lib + iOS targets).
  P2. Runtime core moved to `commonMain` (API migrated to `KClass` + `kotlin.enums`). Android `:inspector` + `:hyperion-plugin` updated for the `KClass` API only, no UI changes.
  P3. DataStore (Okio) storage in `commonMain` with Android/iOS actuals. **Delete `:laboratory:shared-preferences` entirely** (no migration helper — users rewrite their storage wiring to call `dataStore(...)`).
  P4. Gradle plugin rework for KMP + AGP + plain JVM.
  P5. CMP inspector module (`:inspector-cmp`) with iOS `LaboratoryViewController` + Android `@Composable` entry point. Android `:inspector` + `:hyperion-plugin` stay untouched.
  P6. Samples (incl. iOS), docs, publishing (CocoaPods for iOS).

Hyperion stays as an **Android-only `:hyperion-plugin` artifact** pointing at the existing Views `:inspector` — unchanged except for the `KClass` API migration from Phase 2. iOS consumers call `LaboratoryViewController(...)` from Swift; they do not depend on Hyperion or on the Views inspector.

---

## 1. Current module map

Lines-of-code count excludes `build/` and tests unless noted. Public API uses `Class<out Feature<*>>` everywhere.

### 1.1 `:laboratory:runtime` — 19 kt files, ~1413 LOC

**Role:** The actual SDK: `Feature`, `BinaryFeature`, `Storage`, `OptionStorage`, `Laboratory`, `BlockingLaboratory`, `FeatureFactory`, `OptionFactory`, `DefaultOptionFactory`, `DefaultSourceFactory`, `InternalLaboratoryApi`, `internal.FeatureType`.

**Portability inventory:**
| File | Common-ready | Blockers |
|---|---|---|
| `Feature.kt` | mostly yes | none — pure Kotlin interfaces + enum |
| `Storage.kt` | yes | `InMemoryStorage` uses `MutableStateFlow`; works in common |
| `OptionStorage.kt` | no (API-breaking) | `Class<out Feature<*>>` on every method; `storageKey = Class.name`; `findOption` reflects via `optionsRaw`/`options` cache |
| `Laboratory.kt` | no (API-breaking) | same reasons as OptionStorage |
| `BlockingLaboratory.kt` | no | `runBlocking` is JVM/Native but **not** JS; even on Native it requires a worker — ship as `jvmMain` actual or drop |
| `FeatureFactory.kt` | no | `fun create(): Set<Class<out Feature<*>>>` |
| `OptionFactory.kt` | yes once `Class` dependency gone | returns `Feature<*>` which is fine |
| `DefaultOptionFactory.kt` | no | `::class.java.simpleName` + `javaClass` in `check` message |
| `DefaultSourceFactory.kt` | yes | pure |
| `internal/FeatureType.kt` | no | `Class.enumConstants`, `mutableMapOf<Class<*>, List<*>>` cache, `canonicalName`. **This is the one hot spot.** |
| `internal/InternalLaboratoryApi.kt` | yes | annotation only |

**What moves to `commonMain` as-is:** `Feature`, `BinaryFeature`, `DefaultSourceFactory`, `InternalLaboratoryApi`, `Storage` + `InMemoryStorage`, `BlockingIoCall` annotation (annotation itself is portable; the `BlockingLaboratory` body is not).

**What needs changes before `commonMain`:**
- Replace `Class<out Feature<*>>` with `KClass<out Feature<*>>` on every public and internal signature.
- Replace enum discovery (`enumConstants`) with a `commonMain`-safe lookup. Options:
  1. Generated code adds a `KClass.options` extension via `expect/actual` using `enumValues<T>()`. This is clean but needs the generator to emit it per feature or use `kotlin.enums.enumEntries<T>()` which requires a reified `T`.
  2. Have the generator emit a small `KClass<out Feature<*>> → List<Feature<*>>` map at build time (part of the generated `OptionFactory` + `FeatureFactory`), remove runtime reflection entirely. **Preferred.** This also removes the current `mutableMapOf` cache + non-threadsafe `getOrPut`.
- Replace `Class.canonicalName` as storage key: emit `storageKey` in the generated enum (e.g. a `const val storageKey = "io.mehow.sample.AuthType"`), fall back to `simpleName` on K/N consumers.
- `BlockingLaboratory` → optional `jvmMain` actual, with a stub that throws `UnsupportedOperationException` elsewhere, or just drop and let callers use `kotlinx.coroutines.runBlocking` themselves on the JVM/Android side.

### 1.2 `:laboratory:shared-preferences` — 2 kt files, ~136 LOC

**Verdict: delete, no migration helper.** Confirmed with user: DataStore is the one and only storage impl in the new SDK. Users who are on the old `sharedPreferences(...)` factory rewire their storage wiring in their own app code. The 3.0.0 release note calls this out; nothing from this module is carried forward.

### 1.3 `:laboratory:data-store` — 3 kt files, ~145 LOC + 1 `.proto`

**Role:** `DataStoreStorage(DataStore<StorageData>)` + `StorageDataSerializer` + Wire-generated `StorageData` message.

**Blockers:**
- `androidx.datastore:datastore-core` is Android-only.
- `java.io.InputStream`/`OutputStream` in the serializer.
- Wire plugin targets JVM for message classes but Wire itself has multiplatform support (`com.squareup.wire:wire-runtime` is KMP since 4.x; we are on 6.4.0 — fine).

**KMP target:**
- Replace `androidx.datastore:datastore-core` with `androidx.datastore:datastore-core-okio` (KMP, Android + iOS + JVM). Produces a `DataStore<T>` whose `Serializer` is `OkioSerializer<T>` reading/writing `okio.BufferedSource`/`BufferedSink`. Wire's `ProtoAdapter.decode(BufferedSource)` / `.encode(BufferedSink, T)` fits natively, same single `.proto` can stay.
- File path per platform via `expect fun dataStorePath(name: String): okio.Path` (confirmed with user):
  - `androidMain`: `context.filesDir.resolve("datastore/$name.pb")`.
  - `iosMain`: `NSFileManager.default.URL(for: .applicationSupportDirectory, in: .userDomainMask, …)` → `okio.Path`. (Application Support rather than Documents so the file is private and not exposed through iTunes/Files file sharing.)
- Keep public API shape: `Storage.Companion.dataStore(dataStore: DataStore<StorageData>): Storage`. Add convenience factories per platform:
  - Android: `Storage.Companion.dataStore(context: Context, name: String = "laboratory"): Storage`.
  - iOS: `Storage.Companion.dataStore(name: String = "laboratory"): Storage`.

### 1.4 `:laboratory:generator` — 10 kt files, ~1836 LOC

**Role:** Pure Kotlin/JVM, used at build time by the Gradle plugin. KotlinPoet-based codegen for feature enums, `FeatureFactory`, `OptionFactory`.

**KMP relevance:** This module **stays JVM-only** — it runs inside Gradle. But what it **emits** must be `commonMain`-compatible, so:
- `FeatureFactoryModel` currently emits `Class.forName("…")` entries. Replace with KClass references (`AuthType::class`) — KotlinPoet already produces `ClassName`.
- `FeatureFactory.create()` return type must change from `Set<Class<out Feature<*>>>` to `Set<KClass<out Feature<*>>>`.
- Generated `OptionFactory` already uses `%T.%L` which is KMP-safe (plain class references).
- Make generated enums also expose a stable `storageKey` property (either as a constant on the enum class or via a companion). This replaces the reflective `Class.name` key.
- Make the generator emit a `KClass.options`-style lookup table in the generated `FeatureFactory` so the runtime does not have to reflect to list enum entries.

### 1.5 `:laboratory:gradle-plugin` — 23 kt files, ~1392 LOC

**Role:** `io.mehow.laboratory` plugin; registers `generateFeatureFlags`, `generateOptionFactory`, `generateFeatureFactory`; wires them into the Android variant source set or the Kotlin main source set. **Hard-coded to fail on KMP today.**

**Rework required:**
- Remove the KMP guard (`"org.jetbrains.kotlin.multiplatform" -> error(...)`).
- Add a third branch to `ProjectSources`: `ProjectSources.KotlinMultiplatform(KotlinMultiplatformExtension)` → add generated src dir to `commonMain.kotlin` by default; expose DSL to choose a target (`laboratory { targets = setOf("commonMain", "androidMain") }`) if a consumer wants per-target flags later.
- `KotlinJvmCompile::class` task dependency is wrong on KMP. Depend on `KotlinCompilationTask` and let Gradle wire the generated sources via `kotlin.srcDir` (dependencies are inferred).
- `addLaboratoryDependency()` adds to `api` configuration — on KMP there is no top-level `api`; add via `kotlin.sourceSets.commonMain.dependencies.api(...)` when KMP is detected.
- Config-cache compliant already (uses `Property`/`DirectoryProperty`); keep it that way by not referencing `Project` inside task actions.
- Keep working for pure Kotlin JVM and AGP-only modules (the current two paths), add KMP as a third.
- Support variant-specific flags (future-proofing — not required for v1; current plugin iterates `onVariants` and binds per variant for Android, we can keep that for the Android target of a KMP library).

### 1.6 `:laboratory:inspector` — 22 kt files, ~1620 LOC + Android resources

**Role:** Android `LaboratoryActivity` + ViewPager/RecyclerView UI, search, feature grouping, deprecation styling.

**Verdict:** **keep as-is, Android-only.** Confirmed with user: Hyperion continues to open this screen. The only change required is the `KClass` API migration from Phase 2 (every `Class<out Feature<*>>` call site inside the inspector needs updating). No UI port, no deletion. iOS gets its own screen via `:inspector-cmp` (§4). Android then has two possible debug screens (Views via Hyperion, CMP via direct call) which the user explicitly accepted.

### 1.7 `:laboratory:hyperion-plugin` — 2 kt files, 29 LOC

**Role:** AutoService-registered Hyperion plugin that opens `LaboratoryActivity`.

**Verdict:** stays Android-only, no behavioral change. Only touched if the `KClass` API migration in Phase 2 forces compile fixes (it probably doesn't — the plugin just hands context to `LaboratoryActivity.start`).

### 1.8 `:laboratory:testing` — 3 kt files, ~315 LOC

**Role:** JUnit-based `AbstractStorageTest`, feature fixtures, test helpers.

**Verdict:** make it a KMP test library with a `commonTest` `AbstractStorageTest`, `jvmTest`/`androidUnitTest`/`iosTest` facades using `kotlin.test` and `kotest-assertions-core` (both KMP). Keep `turbine` (KMP).

### 1.9 `samples/` — basic, default-option, multi-module, supervision

Each sample pins to the Android artifact. After the migration, keep an Android sample unchanged, add one `kmp-sample/` with Android + iOS targets using the new SDK and CMP inspector.

---

## 2. Gradle plugin rework (detail)

- Make the plugin **target-aware**:
  - `ProjectSources.Kotlin` → unchanged behavior for Kotlin JVM.
  - `ProjectSources.Android` → unchanged, except accept Android targets inside KMP.
  - `ProjectSources.KotlinMultiplatform` → new. Adds generated sources to `kotlin { sourceSets { commonMain { kotlin.srcDir(task) } } }`.
- Dependency wiring:
  - KMP: `kotlin.sourceSets.commonMain.dependencies { api("io.mehow.laboratory:laboratory:$version") }`.
  - Android/Kotlin JVM: keep `dependencies.add("api", …)`.
- Task dependencies: do not reference `KotlinJvmCompile` directly. Use `tasks.withType(KotlinCompilationTask::class.java)` or just register the task provider as a source directory (Gradle will wire it).
- Configuration cache: keep `FeatureFlagInput` `Serializable`; no `Project` references in task actions; `PackageNameProvider` already captures the needed state. Add explicit `@Input` property types instead of relying on `Serializable` for new KMP branches.
- Backwards-compat guardrails:
  - Deprecate the Android-only `shared-preferences` artifact in the generator-emitted DSL.
  - Fail with a clear message when both `com.android.library` and `org.jetbrains.kotlin.multiplatform` are applied but no Android target block exists.
- Keep AGP 8/9 compat: `AndroidComponentsExtension.onVariants` + `variant.sources.kotlin!!` still works for the Android target of a KMP module; verify under `agp-library` 9.2.1 (current catalog pin).

---

## 3. Storage: SharedPreferences → DataStore (KMP)

**Target:** single `:laboratory:runtime` (or `:laboratory`) artifact that exposes `Storage` + `DataStore`-backed implementation on both Android and iOS.

- Dependency: `androidx.datastore:datastore-core-okio:1.2.1` (swap the non-okio variant currently in use) + `com.squareup.wire:wire-runtime:6.4.0` (already KMP).
- Serializer: `object StorageDataSerializer : OkioSerializer<StorageData> { override suspend fun readFrom(source: BufferedSource) = StorageData.ADAPTER.decode(source); override suspend fun writeTo(t, sink) = StorageData.ADAPTER.encode(sink, t) }`.
- Factory:
  ```kotlin
  // commonMain
  internal expect fun dataStorePath(name: String): okio.Path
  fun Storage.Companion.dataStore(name: String = "laboratory"): Storage {
    val ds = DataStoreFactory.create(storage = OkioStorage(FileSystem.SYSTEM, StorageDataSerializer) { dataStorePath(name) })
    return DataStoreStorage(ds)
  }
  ```
- `androidMain`: resolve via `applicationContext.filesDir`; expose convenience `Storage.Companion.dataStore(Context, String)`.
- `iosMain`: use `NSFileManager.defaultManager.URLForDirectory(NSApplicationSupportDirectory, …)` → `okio.Path`.
- **No migration helper from SharedPreferences.** Confirmed with user: `:laboratory:shared-preferences` is deleted outright; existing Android consumers rewire their `Storage` wiring to `dataStore(...)` and lose the previously selected flags (acceptable because the inspector lets them re-pick, and this is a debug-only concern).

---

## 4. Packaging + iOS consumption

**Target shape (confirmed with user):**
- `:laboratory:shared` (new, replaces `:laboratory:runtime` + `:laboratory:data-store` + `:laboratory:shared-preferences`) — one KMP library exporting `Feature`, `Storage`, DataStore storage factory, `Laboratory`, `OptionFactory`, `FeatureFactory`, codegen-friendly API. Published as `io.mehow.laboratory:laboratory:3.0.0` with iOS klibs + Android AAR + JVM jar. Module directory name is `:shared` because the user asked for that structure.
- `:laboratory:generator` — unchanged, JVM.
- `:laboratory:gradle-plugin` — unchanged publishing coordinates (`io.mehow.laboratory` plugin id).
- `:laboratory:inspector` — unchanged, Android-only, Views-based. Only touched by the `KClass` API migration from Phase 2.
- `:laboratory:hyperion-plugin` — unchanged, Android-only, depends on `:inspector` + Hyperion.
- `:laboratory:inspector-cmp` — new KMP module (Android + iOS targets). Depends on CMP + `:shared`. Published for **both Android and iOS** so Android consumers that skip Hyperion can also use it directly.

**iOS consumption (CocoaPods, user-confirmed):**
- Use the `kotlin("native.cocoapods")` plugin on `:shared` and `:inspector-cmp` to generate podspecs during build. iOS consumers add two pods (or we expose one umbrella pod that depends on both — decide at Phase 6):
  - `pod 'Laboratory'` → the SDK (`:shared`).
  - `pod 'LaboratoryInspector'` → the Compose MP debug screen (`:inspector-cmp`), typically pulled only in a debug Podfile stanza.
- Framework base names: `Laboratory` and `LaboratoryInspector`. One `import Laboratory` + `import LaboratoryInspector` in Swift.
- `LaboratoryInspector` exports `LaboratoryViewController(configuration): UIViewController` as the only public Swift-facing API. Consumers call it from, e.g., a settings screen when `#if DEBUG`.

---

## 5. iOS debug screen — three options compared

### Option A (recommended): Compose Multiplatform inspector exposed as `UIViewController`

- New module `:laboratory:inspector-cmp` with `commonMain` holding the entire Compose UI (list of features, chips for options, search bar, source toggle, deprecation styling).
- `iosMain` has one entry point:
  ```kotlin
  fun LaboratoryViewController(configuration: LaboratoryConfiguration): UIViewController =
      ComposeUIViewController { LaboratoryInspector(configuration) }
  ```
- Swift usage:
  ```swift
  let vc = LaboratoryViewController(configuration: ...)
  present(vc, animated: true)
  ```
- `androidMain` exposes `@Composable fun LaboratoryInspector(configuration: LaboratoryConfiguration)` plus a thin `LaboratoryActivity` wrapping it in a `ComposeView` (so Hyperion keeps working with one method call).
- **Trade-offs:** Pulls Compose MP (~2–3 MB on iOS) into every iOS consumer; this is debug-only code so most teams gate it behind a debug flag or SPM `#if DEBUG`. Single codebase, zero duplicate UI, parity with the current Android inspector by construction.

### Option B: SwiftUI package for iOS, Compose Views for Android

- Core (`:laboratory`) exposes a `LaboratoryViewModel`-style KMP class (`experiment`, `observe`, factories), the UI is written twice.
- **Trade-offs:** No Compose MP dependency. Keeps iOS consumers purely SwiftUI. But: twice the UI code to maintain, drift risk, and the user wants "one method call to show the screen" — fine either way, but we still have to ship + version a Swift package in addition to the KMP SDK.

### Option C: Shared CMP UI for **both** platforms

- Same as A, but Android also loses the Views-based inspector and uses CMP. This is already how A works; the only difference is whether we keep the current Views inspector around in parallel.
- **Trade-offs:** cleanest, but a bigger single PR. Mitigated by phase separation (keep Views inspector in place until CMP inspector reaches parity, then remove in a single commit).

**Decision:** Option **A → C** (ship A first, follow up with C by deleting the Views inspector once Hyperion is retargeted). This matches the user's requirement: `fun LaboratoryViewController(...): UIViewController` for iOS, Hyperion-equivalent screen for Android, no duplicate UI long-term.

---

## 6. Risks (ranked)

1. **Breaking API: `Class` → `KClass`.** Every consumer must recompile. We own this via a 3.0.0 release and migration notes. Also means the Hyperion plugin and samples need updates.
2. **Enum reflection drift.** The right fix is to delete the reflection path entirely and let the Gradle plugin emit explicit option lists. The caching + `getOrPut` on a plain `mutableMapOf` is already racy today — removing it is a bugfix.
3. **DataStore migration.** We must ship the one-shot `SharedPreferencesMigration` or Android users lose state when upgrading. Add an integration test that reads a prefabricated old-format `SharedPreferences` file and asserts the DataStore state.
4. **`java.io` in generated code / factories.** `OptionFactoryModel` already emits portable code; `FeatureFactoryModel` emits `Class.forName(...)` which is JVM-only. Replace with `TypeName`/`KClass` references.
5. **Config cache + task wiring under KMP.** The current plugin binds generated sources via two mutually exclusive branches; KMP introduces a third. Verify under AGP 9.2.1 + KGP 2.3.21 that `onVariants` still fires for the Android target of a KMP library and that we only call `kotlin.srcDir` once.
6. **Compose MP size on iOS.** ~3 MB stripped. Acceptable for a debug-only SDK; document how to gate it behind a build config and expose a `laboratoryStub` artifact for release builds that returns a no-op `UIViewController`.
7. **Test coverage.** The existing JVM tests (`AbstractStorageTest`, `Laboratory`/`OptionStorage` tests, Robolectric tests for SharedPreferences) are a good safety net for logic parity. Keep them all; add `commonTest` versions that run under `kotlin-test` + `turbine` + `coroutines-test` (both KMP). Add iOS test runs for the DataStore storage.

---

## 7. Conventions / decisions

All decisions below are confirmed with the user unless marked `[open]`.

- **Group / coordinates:** keep `io.mehow.laboratory`; bump to `3.0.0` for the KMP release. Snapshot track: `3.0.0-SNAPSHOT`.
- **Module layout:** `:laboratory:shared` (KMP SDK, the one SDK artifact), `:laboratory:generator` (JVM, build-time only), `:laboratory:gradle-plugin` (JVM Gradle plugin), `:laboratory:inspector` (Android-only, Views, kept), `:laboratory:hyperion-plugin` (Android-only, kept), `:laboratory:inspector-cmp` (KMP, Android + iOS, new).
- **One SDK artifact:** `io.mehow.laboratory:laboratory:3.0.0` (produced by `:laboratory:shared`). No separate runtime / data-store / shared-preferences artifacts in 3.x. `:laboratory:shared-preferences` is deleted outright, no migration helper.
- **iOS distribution:** CocoaPods via `kotlin("native.cocoapods")`. Framework base names `Laboratory` and `LaboratoryInspector`.
- **Public API uses `KClass<out Feature<*>>`.** `Class`-based overloads are not kept; call sites change to `AuthType::class`. The Android `:inspector` + `:hyperion-plugin` are updated to the new API (compile-only change, no UI rewrite).
- **`storageKey` is generated.** Each generated enum gets `const val storageKey: String` (or a companion property) so the runtime no longer calls `Class.canonicalName`. The default is `className.canonicalName` captured at generation time, keeping DataStore schema stable for consumers already on DataStore today.
- **`BlockingLaboratory` stays JVM-only.** Lives in `jvmMain`/`androidMain` as a concrete class (as a `jvmMain` extension `fun Laboratory.blocking(): BlockingLaboratory`); not re-exported from `commonMain`.
- **CMP inspector is Material 3** (`compose.material3`).
- **CMP + Kotlin pinning:** `:inspector-cmp` may pin to a slightly older Kotlin than the rest of the project if whatever CMP release is current at Phase 5 start does not yet support Kotlin 2.3.x. Isolated to that module via a build-logic override; the SDK stays on the main Kotlin version. `[open — resolved at Phase 5 start]`
- **No new DSL features in the Gradle plugin.** The existing extension (`feature { }`, `enabledFeature`, `disabledFeature`, `featureFactory`, `optionFactory`, `dependency`) keeps its signatures. The only addition is auto-detection of KMP.
- **Kotlin stdlib APIs** we may freely use (K/N stable): `kotlin.enums.enumEntries<T>()` (reified), `kotlin.reflect.KClass`. We're on Kotlin 2.3.21, all fine.
- **Thread-safety primitive:** `kotlinx-atomicfu` (already KMP). Used in the inspector's metadata loader cache (replaces `java.util.concurrent.atomic.AtomicReference`).
- **DataStore file paths:** Android `context.filesDir.resolve("datastore/laboratory.pb")`, iOS `<Application Support>/laboratory.pb`. Default name is `"laboratory"`, overridable by the consumer.

---

## 8. Phased plan (one agent session each)

> Each phase is self-contained. The acceptance criterion is "builds + tests pass locally (`./gradlew check`) and the diff is reviewable in one sitting". Mark phase checkboxes as the work completes.

### Phase 1 — Workspace plumbing + `:shared` KMP scaffold
Scope: ~1 day. **No logic ported yet.**

- [x] Add Kotlin Multiplatform plugin to the version catalog and convention plugin (`kotlin("multiplatform")` alias).
- [x] Add Compose Multiplatform plugin alias to the catalog (`org.jetbrains.compose`) and the CocoaPods plugin alias (`kotlin("native.cocoapods")`) — referenced by `:inspector-cmp` later.
- [x] Create `:laboratory:shared` module with KMP + Android library + iosX64/iosArm64/iosSimulatorArm64 targets. Empty source sets, dummy `expect fun platform(): String` + `actual` on each platform just to prove the toolchain works.
- [x] Verify publishing dry-run (`./gradlew publishToMavenLocal`) produces `laboratory`, `laboratory-android`, `laboratory-iosarm64`, `laboratory-iosx64`, `laboratory-iossimulatorarm64`, `laboratory-jvm` modules.
- [x] Keep the existing `:runtime`, `:data-store`, `:shared-preferences`, `:inspector`, `:hyperion-plugin` modules intact and buildable.
- **Acceptance:** `./gradlew :laboratory:shared:build` passes; `./gradlew build` on everything else still green; nothing published to Maven Central.

### Phase 2 — Port runtime core to `commonMain` (+ propagate `KClass` to Android inspector)
Scope: ~1 day.

- [ ] Move `Feature.kt`, `BinaryFeature.kt`, `DefaultSourceFactory.kt`, `InternalLaboratoryApi.kt`, `Storage.kt` (+ `InMemoryStorage`), `BlockingIoCall` annotation to `:shared/commonMain`.
- [ ] Migrate `OptionStorage`, `Laboratory`, `OptionFactory`, `FeatureFactory`, `DefaultOptionFactory` from `Class<out Feature<*>>` to `KClass<out Feature<*>>`.
- [ ] Replace `internal/FeatureType.kt` reflection-based option lookup with a `KClass<out Feature<*>> → List<Feature<*>>` map populated by the generator (temporarily hand-roll a small helper for tests until Phase 4 wires the generator).
- [ ] Introduce `Feature.storageKey: String` with a default implementation returning `this::class.simpleName!!` (temporary until generator emits a stable key).
- [ ] Move `BlockingLaboratory` to `jvmMain` as a `fun Laboratory.blocking(): BlockingLaboratory` extension.
- [ ] Update `:laboratory:inspector` + `:laboratory:hyperion-plugin` to compile against the new `KClass`-based API (`FeatureFactory.create()` returning `Set<KClass<...>>`, `OptionStorage` methods taking `KClass<...>`, etc.). No UI changes.
- [ ] Port existing JVM tests to `commonTest` where possible; keep the JVM-specific ones in `jvmTest`.
- **Acceptance:** `:laboratory:shared` compiles and tests pass on `jvm`, `android`, `iosSimulatorArm64`. In-memory `Laboratory.inMemory()` works from a `commonTest` test. Android `:inspector` + `:hyperion-plugin` build green against the new API.

### Phase 3 — DataStore (Okio) storage in `:shared` + delete `:shared-preferences`
Scope: ~1 day.

- [ ] Swap `androidx.datastore:datastore-core` for `androidx.datastore:datastore-core-okio` in the version catalog.
- [ ] Keep the single `storage_data.proto` (shared Wire codegen target in `commonMain`).
- [ ] Reimplement `DataStoreStorage` + `StorageDataSerializer` on `OkioSerializer<StorageData>`.
- [ ] Add `expect fun dataStorePath(name: String): okio.Path` + Android/iOS/JVM actuals. Expose `Storage.Companion.dataStore(context: Context, name: String = "laboratory")` in `androidMain` and `Storage.Companion.dataStore(name: String = "laboratory")` in `iosMain`.
- [ ] Delete `:laboratory:shared-preferences` entirely. Update any sample using it to call `dataStore(context)` instead.
- [ ] Delete the old `:laboratory:data-store` module after its contents have moved into `:shared`.
- **Acceptance:** `AbstractStorageTest` runs green in `commonTest` with `InMemoryStorage` and `DataStoreStorage` (tmp dir). Android + iOS smoke tests write and read a flag through the real DataStore on each platform.

### Phase 4 — Gradle plugin + generator rework for KMP
Scope: ~1 day.

- [ ] Delete the KMP guard in `LaboratoryPlugin.kt`; add `ProjectSources.KotlinMultiplatform`.
- [ ] Register the generated source directory via `kotlin.sourceSets.commonMain.kotlin.srcDir(task)`; keep the existing Android/Kotlin JVM branches.
- [ ] Replace `KotlinJvmCompile` dependency wiring with implicit Gradle wiring via `srcDir(TaskProvider)`.
- [ ] Teach the generator to emit:
  - a `const val storageKey = "<fqcn>"` on every generated enum,
  - `FeatureFactory.create()` returning `Set<KClass<out Feature<*>>>`,
  - a generated `KClass<out Feature<*>>.options: List<Feature<*>>` lookup table that the runtime consumes directly (replaces `FeatureType.kt` reflection).
- [ ] Teach the plugin's `addLaboratoryDependency` to detect KMP and add to `commonMain.dependencies.api(...)`.
- [ ] Update the integration tests in `:gradle-plugin` to cover a KMP consumer (`KotlinMultiplatformIntegrationTest`).
- **Acceptance:** `:gradle-plugin:test` is green with new coverage. Applying `io.mehow.laboratory` to a KMP sample module generates `commonMain` sources that compile on all targets.

### Phase 5 — Compose Multiplatform inspector (`:inspector-cmp`)
Scope: ~1.5 days (biggest UI phase).

- [ ] Create `:laboratory:inspector-cmp` KMP module (Android + iOS targets) with `org.jetbrains.compose` plugin + `compose.runtime`, `compose.foundation`, `compose.material3`, `compose.ui`. If current CMP release does not support Kotlin 2.3.21, pin this module to the newest Kotlin that CMP supports (isolated override).
- [ ] Build the Compose UI in `commonMain`: feature-section tabs, search bar, option chips, source toggle, deprecation styling, Markdown link rendering (`TextToken`). This is a fresh Compose implementation that mirrors the existing Android inspector's behavior; it does **not** have to be a line-for-line port.
- [ ] Reuse `FeatureMetadata`, `ToolbarViewModel`-style logic where portable (move needed helpers to `:shared/commonMain` or duplicate here if the code is UI-shaped). Replace `AtomicReference` with `kotlinx-atomicfu`.
- [ ] Preserve a `LaboratoryConfiguration.builder()` API analogous to the Android `LaboratoryActivity.Configuration`.
- [ ] `androidMain` exposes `@Composable fun LaboratoryInspector(configuration: LaboratoryConfiguration)` for consumers that skip Hyperion. **No change to the existing `:inspector` module or Hyperion.**
- [ ] `iosMain` exposes `fun LaboratoryViewController(configuration: LaboratoryConfiguration): UIViewController` via `ComposeUIViewController`.
- [ ] Configure the `kotlin("native.cocoapods")` block with `framework { baseName = "LaboratoryInspector" }`.
- **Acceptance:** iOS sample Swift code calls `LaboratoryViewController(...)` and presents the screen. An Android sample with no Hyperion dependency also hosts `LaboratoryInspector(...)` in a `ComponentActivity` + `ComposeView`.

### Phase 6 — Samples, docs, publishing
Scope: ~0.5–1 day.

- [ ] Add `samples/kmp` with Android + iOS targets using the new SDK and CMP inspector.
- [ ] Verify the existing Android `samples/basic`, `samples/default-option`, `samples/multi-module`, `samples/supervision` still build against the new `KClass` API + Hyperion/Views inspector unchanged.
- [ ] Update docs (`docs/` + MkDocs config) to describe iOS usage, `KClass`-based API, and the deletion of `:shared-preferences`.
- [ ] Finalize CocoaPods publishing for both `:shared` (`baseName = "Laboratory"`) and `:inspector-cmp` (`baseName = "LaboratoryInspector"`).
- [ ] Update `README.md` TLDR to show both Android and iOS snippets.
- [ ] Dry-run publish to `mavenLocal` + a local pod repo; verify every artifact (`laboratory`, `inspector-cmp`, `hyperion-plugin`, `generator`, `gradle-plugin`) resolves correctly.
- **Acceptance:** fresh KMP sample app on both platforms uses the published-to-local SDK and shows the inspector screen; Android sample via Hyperion still works.

---

## 9. Open questions (resolved)

All resolved by the user. Decisions carried into §7 Conventions.

1. **Legacy `shared-preferences`:** DELETE outright, no migration helper. DataStore is the only storage. → §1.2, §3.
2. **iOS distribution:** CocoaPods via `kotlin("native.cocoapods")`. → §4.
3. **Inspector theme:** Material 3. → §7.
4. **Compose MP vs Kotlin 2.3.21 pinning:** isolate `:inspector-cmp` on a compatible Kotlin version if needed at Phase 5 start; the rest of the project stays on 2.3.21. → §7.
5. **`BlockingLaboratory`:** keep as a `jvmMain` extension `fun Laboratory.blocking(): BlockingLaboratory`. Not available on iOS. → §7.
6. **`:inspector-cmp` targets:** publish for Android + iOS. Android then has two possible debug screens (Views via Hyperion + CMP via direct call). User-accepted. → §4.
7. **DataStore paths:** Android `filesDir/datastore/laboratory.pb`, iOS `<Application Support>/laboratory.pb`. → §3, §7.

---

## 10. Progress tracker

- [x] Phase 0 — Analysis + plan (this document)
- [x] Phase 1 — Workspace plumbing + `:shared` KMP scaffold
- [ ] Phase 2 — Port runtime core to `commonMain` (+ KClass propagation into Android inspector)
- [ ] Phase 3 — DataStore (Okio) storage + delete `:shared-preferences` + delete `:data-store`
- [ ] Phase 4 — Gradle plugin + generator rework for KMP
- [ ] Phase 5 — Compose Multiplatform inspector (`:inspector-cmp`)
- [ ] Phase 6 — Samples, docs, publishing

Session notes go **below this line** — the agent for each phase appends a brief log (date, what changed, next tripwire) so the next session resumes with context.

---
### Phase 1 — 2026-10-17
- Added plugin aliases to `gradle/libs.versions.toml`: `kotlin-multiplatform`, `kotlin-cocoapods`, `compose-multiplatform` (pinned to `1.9.0`, flagged `TODO verify at Phase 5`), `compose-compiler`, plus `agp-kotlin-multiplatform-library` (`com.android.kotlin.multiplatform.library`). The last one was unavoidable: AGP 9.0+ refuses to apply plain `com.android.library` alongside `org.jetbrains.kotlin.multiplatform` and tells you to use the KMP-aware Android library plugin.
- Declared `kotlin-multiplatform` and `agp-kotlin-multiplatform-library` at the root `build.gradle.kts` with `apply false` (consistent with how `kotlin.jvm` and `agp.library` were already handled).
- Added `:laboratory:shared` to `settings.gradle.kts` (kept every existing include line).
- Created `laboratory/shared/` with:
  - `build.gradle.kts` applying `kotlin-multiplatform`, `agp-kotlin-multiplatform-library`, `maven-publish`, `dokka`, and the convention plugin. Targets: `jvm()`, `android { … }` (the new KMP-native Android library DSL — not `androidLibrary`, which is deprecated in current AGP), `iosX64()`, `iosArm64()`, `iosSimulatorArm64()`. `commonTest` pulls `kotlin("test")`.
  - `commonMain/Platform.kt` with `internal expect fun platform(): String`.
  - `androidMain`/`jvmMain` actuals returning `"android"` / `"jvm"`.
  - `iosMain/Platform.kt` returning `"ios-<cpu arch>"` via `kotlin.native.Platform.cpuArchitecture` (opt-in `ExperimentalNativeApi`). All three iOS targets share this via the default KMP hierarchy template.
  - `commonTest/PlatformTest.kt` asserting `platform()` is non-empty.
  - `api/jvm/shared.api` empty baseline file created via `./gradlew :laboratory:shared:apiDump` so `binary-compatibility-validator` has something to compare against.
- Convention plugin tweak (`build-logic/src/main/kotlin/io/mehow/laboratory/convention/ConventionPlugin.kt`): changed `tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>()` to `KotlinCommonCompilerOptions`. The old form was fine for JVM but, under KMP, native compile tasks match the raw `KotlinCompilationTask` type and the configure block was then cast to `KotlinJvmCompilerOptions`, which blew up with `KotlinNativeCompilerOptionsDefault_Decorated cannot be cast to KotlinJvmCompilerOptions`. The three options we read (`progressiveMode`, `allWarningsAsErrors`, `optIn`) all exist on the common base, so the switch is behaviour-preserving for the existing modules.
- Verified:
  - `./gradlew :laboratory:shared:build --no-configuration-cache` → BUILD SUCCESSFUL, all iOS, JVM, and Android compile+test tasks ran green (iosSimulatorArm64Test ran, iosX64Test SKIPPED on arm64 host as expected).
  - `./gradlew :laboratory:shared:assemble` → BUILD SUCCESSFUL with configuration cache stored.
  - `./gradlew :laboratory:shared:publishToMavenLocal --no-configuration-cache` → BUILD SUCCESSFUL. Published artifacts under `~/.m2/repository/io/mehow/laboratory/` are `shared`, `shared-android`, `shared-jvm`, `shared-iosarm64`, `shared-iosx64`, `shared-iossimulatorarm64`, all at version `2.0.4-SNAPSHOT` (from `gradle.properties`). The migration plan mentions `laboratory:3.0.0` as the eventual target coordinates — artifact-id and version bump are Phase 6, not Phase 1.
  - `./gradlew build --no-configuration-cache -x :laboratory:gradle-plugin:validatePlugins -x :laboratory:gradle-plugin:test -x :laboratory:gradle-plugin:check` → BUILD SUCCESSFUL (all other modules build + their tests pass).
- **Tripwires for Phase 2:**
  - `./gradlew :laboratory:gradle-plugin:validatePlugins` fails on `trunk` with task-validation errors (`OptionFactoryTask` / `OutputTask` missing `@CacheableTask`/`@DisableCachingByDefault` and output-annotation errors). This is pre-existing — my changes don't touch `laboratory/gradle-plugin/src/`. Likely surfaced by the recent Gradle 9.5.1 / AGP 9.2.1 / Kotlin 2.3.21 bumps. Phase 4 rewrites the plugin anyway, so leaving it for then — but Phase 2 should run `check` with that task excluded (as done above) until Phase 4 lands.
  - Configuration cache serialization fails for `:*:build` task graphs across every module (new `:shared` AND existing `:runtime`, same error: `com.diffplug.spotless.ConfigurationCacheHackList` / `stepsInternalEquality` on `spotlessKotlin`). The config cache stores successfully for narrower task graphs (e.g. `:laboratory:shared:assemble`, `:laboratory:shared:spotlessKotlinCheck`). Also pre-existing; not caused by this phase. The hint in the error (`Add a step with [com.facebook:ktfmt:0.63] into the spotlessPredeclare block in the root project`) points at Spotless 8.6.0's predeclare machinery colliding with the way the convention plugin wires `ktfmt(...).googleStyle()`. Phase 2 should either confine CI to narrower task graphs or spend 15 minutes untangling the predeclare — easier once there's actual Kotlin code in `commonMain` and the exact failure mode is clearer.
  - KMP + the new `com.android.kotlin.multiplatform.library` plugin uses the Kotlin-side DSL (`kotlin { android { … } }` with `withHostTestBuilder {}` / `withDeviceTestBuilder {}`), **not** the classic `com.android.library` block. Phase 2 shouldn't try `android { … }` at the top level of the `:shared` build file — it won't resolve.
  - Default KMP hierarchy auto-creates `iosMain` and shares the three iOS targets' source sets. No explicit `sourceSets { val iosMain by creating }` needed. Phase 3's `expect fun dataStorePath` can rely on it.
  - `apiDump` had to be run once to seed `laboratory/shared/api/jvm/shared.api` (empty). Any new public API added in Phase 2 will need another `apiDump` or the `apiCheck` will fail.
  - `publishToMavenLocal` currently publishes under artifact-id `shared`, not `laboratory`. If Phase 2 ports `:runtime` sources into `:shared` and consumers still read `io.mehow.laboratory:laboratory:…`, publishing coordinates will need to be set explicitly (via `mavenPublishing { coordinates(...) }` or by renaming the Gradle project).
