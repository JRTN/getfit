# 6. Build and quality

## Philosophy

- **One place for build configuration.** Everything modules share lives in convention plugins in [`build-logic/`](../../build-logic/), so a module build file is usually one to five lines.
- **Rules are automated.** If a convention matters, a tool checks it: a compiler error, a Konsist test, a detekt rule, a unit test or a Gradle check. Code review is for judgment, not policing.
- **The build explains itself.** Every convention plugin and build-logic helper has KDoc saying what it does and *why*. Read them; they are short.
- **Versions are pinned in one file,** [`gradle/libs.versions.toml`](../../gradle/libs.versions.toml). Upgrades are deliberate commits, never drift.

## Convention plugins

Each lives in [`build-logic/convention/src/main/kotlin/`](../../build-logic/convention/src/main/kotlin/):

| Plugin ID | Class | Applied by | What it sets up |
| --- | --- | --- | --- |
| `app.jvm.library` | [`JvmLibraryConventionPlugin`](../../build-logic/convention/src/main/kotlin/JvmLibraryConventionPlugin.kt) | pure-Kotlin modules, `:core:testing`, `:architecture-tests`, `:detekt-rules` | Kotlin/JVM, JVM 17 bytecode, JUnit 5 and AssertK |
| `app.android.library` | [`AndroidLibraryConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt) | every Android library | AGP library, SDK levels, namespace from module path, JUnit 4 and AssertK |
| `app.android.library.compose` | [`AndroidLibraryComposeConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt) | design system, UI | the above plus the Compose compiler and BOM |
| `app.android.feature` | [`AndroidFeatureConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt) | `:feature:*` | Compose and Hilt, plus *exactly* the four allowed core dependencies, Navigation 3, serialization and the ViewModel test stack |
| `app.android.room` | [`AndroidRoomConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidRoomConventionPlugin.kt) | `:core:database` (later `:core:reference`) | KSP, the Room Gradle plugin with schema export to `schemas/`, Room test helpers and Robolectric |
| `app.hilt` | [`HiltConventionPlugin`](../../build-logic/convention/src/main/kotlin/HiltConventionPlugin.kt) | `:app`, `:core:data`, `:core:sync`, features | KSP plus Hilt (no kapt) |
| `app.android.application` | [`AndroidApplicationConventionPlugin`](../../build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt) | `:app` | `applicationId`, version, the `.debug` suffix, R8 on release, signing |
| `app.quality` | [`QualityConventionPlugin`](../../build-logic/convention/src/main/kotlin/QualityConventionPlugin.kt) | applied by all the others | ktlint, detekt with GetFit's rules, and Android Lint |

Shared helpers live in [`com/jrussell/fitness/buildlogic/`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/):

| Helper | What it does |
| --- | --- |
| [`AndroidCommon.kt`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/AndroidCommon.kt) | SDK constants; derives the namespace from the module path (`:feature:nutrition` → `com.jrussell.fitness.feature.nutrition`); adds the Robolectric JVM flag |
| [`GitVersionCode.kt`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/GitVersionCode.kt) | `versionCode` = git commit count (so every later build installs over an earlier one), configuration-cache safe, with a fallback when git is unavailable |
| [`ReleaseSigning.kt`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/ReleaseSigning.kt) | Signs release builds from properties outside the repo, or leaves them unsigned |
| [`ProjectExtensions.kt`](../../build-logic/convention/src/main/kotlin/com/jrussell/fitness/buildlogic/ProjectExtensions.kt) | `libs.library("…")` / `libs.pluginId("…")` catalog lookups by string. Type-safe catalog accessors do not work inside build-logic's own plugins |

**AGP 9 built-in Kotlin:** the Android Gradle Plugin compiles Kotlin itself. Never apply `org.jetbrains.kotlin.android`.

## Versioning

- `versionName` is set by hand in `AndroidApplicationConventionPlugin.VERSION_NAME` (currently `0.0.1`). `versionCode` is the commit count.
- Releases are tagged `vX.Y.Z` on `master`:
  - **0.1.0** is the first usable app (the end of M1);
  - each newly useful capability bumps the minor version;
  - fixes bump the patch version;
  - **1.0.0** means the original design is complete.
- Development is trunk-based. Each plan gets a short-lived branch, fast-forward merged into `master` when the full suite is green.

## Every automated check

`./gradlew check` runs all of these, then `./gradlew buildHealth` runs dependency analysis.

| Check | Tool | What it catches |
| --- | --- | --- |
| Formatting | ktlint (Android Studio style, [`.editorconfig`](../../.editorconfig)) | Indentation, imports, wrapping. Function expression bodies are disabled to keep the explicit block-body style |
| Code smells | detekt 2 ([`config/detekt/detekt.yml`](../../config/detekt/detekt.yml)) | Long lines, magic numbers, too many returns, swallowed exceptions… |
| GetFit rules | detekt rule set `getfit` ([`:detekt-rules`](../../detekt-rules/), switched on in [`config/detekt/getfit.yml`](../../config/detekt/getfit.yml)) | `Color` literals outside the design system; direct `Dispatchers.IO/Default/Main/Unconfined` outside `:app`'s `di` package; any `fallbackToDestructiveMigration…` |
| GetFit rule config | [`GetFitConfigTest`](../../detekt-rules/src/test/kotlin/com/jrussell/fitness/detekt/GetFitConfigTest.kt) | `getfit.yml` names exactly the provided rules and activates each one. detekt does not validate plugin config, so a typo would otherwise silently disable a rule |
| Android Lint | AGP, `abortOnError = true` | Android-specific problems (accessibility, API levels, resources). Version-freshness checks are off, since upgrades are deliberate |
| Architecture | Konsist ([`:architecture-tests`](../../architecture-tests/)) | Layer rules, ViewModel dependencies, owner-scoped DAOs (see [Architecture](02-architecture.md)) |
| Unit tests | JUnit 4/5, Robolectric | Behavior (see [Conventions and testing](07-conventions-and-testing.md)) |
| Contrast | [`PaletteContrastTest`](../../core/designsystem/src/test/kotlin/com/jrussell/fitness/core/designsystem/theme/PaletteContrastTest.kt) | WCAG ratios for all six palette instances |
| Schema | [`SchemaJsonTest`](../../core/database/src/test/kotlin/com/jrussell/fitness/core/database/SchemaJsonTest.kt) | Committed schema JSON matches the compiled database |
| Dependencies | Dependency Analysis Gradle Plugin (root [`build.gradle.kts`](../../build.gradle.kts)) | `implementation` that should be `api` (or the reverse), undeclared transitive use. Unused dependencies are only warnings while features are empty |

### Adding a detekt rule

1. Write the rule in `detekt-rules/src/main/kotlin/…` (extend `dev.detekt.api.Rule`) with tests using `rule.lint(code)`.
2. Register it in [`GetFitRuleSetProvider`](../../detekt-rules/src/main/kotlin/com/jrussell/fitness/detekt/GetFitRuleSetProvider.kt).
3. Add `NewRule: active: true` to `config/detekt/getfit.yml`. `GetFitConfigTest` fails until you do.

## CI

[`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) runs `check`, `buildHealth` and `assembleDebug` on every push. The repository is local-only for now, so the workflow is dormant; it has no effect on local builds. It checks out full history because `versionCode` is the commit count.

## Gradle performance settings

[`gradle.properties`](../../gradle.properties) turns on the configuration cache, the build cache and parallel execution, and gives Gradle 4 GB. The configuration cache is why build logic reads git and properties through Gradle `Provider`s and `ValueSource`s rather than directly.

Next: [Conventions and testing](07-conventions-and-testing.md).
