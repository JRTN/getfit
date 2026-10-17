# M1a — Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the M0 theme stub with GetFit's real design system: a violet-accented neutral palette in three variants (Default, Color-vision-safe, High contrast) × light/dark with WCAG contrast tests; Inter with tabular figures; 2–4 dp shapes; Material Symbols icons; and a Settings → Appearance screen whose choices persist in DataStore and restyle the app immediately.

**Architecture:** `:core:designsystem` owns every color, the type scale, shapes and icons. It provides a Material `ColorScheme` plus an app-specific `DataColors` through `LocalDataColors`. The user's theme mode and palette are UI preferences: they are stored in Preferences DataStore (`:core:datastore`), exposed through a `UserPreferencesRepository` interface (`:core:domain`, implemented in `:core:data`, bound in `:app`), and read by `MainViewModel` and `SettingsViewModel`. A custom detekt rule enforces "no `Color(` outside `:core:designsystem`".

**Tech Stack:** Compose Material 3 (BOM 2026.09.00), Preferences DataStore 1.2.1, Inter 4.1 (static TTF, OFL), Material Symbols Outlined (Android vector drawables), detekt 2.0.0-alpha.6 API, Robolectric 4.17 + Compose UI test, Turbine 1.2.1, kotlinx-coroutines-test 1.11.0.

**Spec:** `docs/initial_design.md`, sections "Design system and accessibility", "Module architecture" (dependency rules), "Build infrastructure" (custom static-analysis rules) and "v1 milestone plan" (M1). This is plan 1 of 4 for M1 (M1a design system → M1b data foundation → M1c weight, metrics and charts → M1d backup).

## Global Constraints

- Branch `m1a-design-system`, cut from `master` at `v0.0.1`, merged back when green (decision 36). Do not push; the repository is local-only.
- `minSdk` 34, `targetSdk` 37, `compileSdk` 37.
- Kotlin explicit style: explicit types, block bodies, named classes. KDoc on every convention plugin and build-logic helper.
- "No composable outside `:core:designsystem` uses a `Color(...)` literal; detekt enforces it."
- "Dynamic color: off by default ... so chart colors stay predictable."
- "Elevation: `tonalElevation = 0.dp` everywhere; separation by hairline dividers."
- "Corners: 2–4 dp."
- "Typeface: Inter ... bundled; `fontFeatureSettings = "tnum"` on all numeric text."
- "Density: Compact visual rows; every interactive element keeps a ≥ 48 dp touch target via `minimumInteractiveComponentSize()`."
- "A unit test computes the WCAG contrast ratio of every foreground/background token pair in all six instances and requires ≥ 4.5:1 for text and ≥ 3:1 for graphical elements."
- Color is never the only encoding (redundant-encoding table in the spec); this plan adds no color-only signals.
- Every user-visible string is a resource.
- `:feature:*` depends only on `:core:domain`, `:core:model`, `:core:ui`, `:core:designsystem`. `:core:data` is the only module that depends on `:core:datastore`. `:app` is the only module that binds implementations to interfaces.
- Run Gradle with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`; in Git Bash set `MSYS_NO_PATHCONV=1` for `adb shell` paths.

## Decisions recorded by this plan (user, 2026-10-16)

- **Typeface:** Inter (closes the design's "Inter or IBM Plex Sans").
- **Accent:** violet, `#6941C6` in light and `#C3B1FF` in dark (closes open question "Accent hue for the default palette"). The user picked it from the accent picker mockups.
- **M1 split:** four sub-plans, in order M1a → M1b → M1c → M1d. `v0.1.0` is tagged after M1d.

## Calls made in this plan

1. **High contrast uses stricter thresholds:** ≥ 7:1 for text and ≥ 4.5:1 for graphics. The spec's 4.5/3 is the floor for the other two palettes.
2. **Contrast is checked against both `background` and `surfaceContainer`,** because nav bars, cards and sheets sit on the container color.
3. **The color-vision-safe palette keeps the violet UI accent.** Only `DataColors` switches to Okabe–Ito hues, shifted in lightness to meet the thresholds. Status colors always carry a second cue.
4. **True-black dark mode stays deferred** (the spec marks it optional). Custom detekt rules for `Dispatchers.*` and `fallbackToDestructiveMigration` move to M1b, which creates the dispatcher module and Room code they guard.
5. **Contrast tests run on JUnit 4** in `:core:designsystem`, an Android module (spec: "JUnit 4 + Robolectric (Android modules)"). The math is plain Kotlin with no Robolectric needed.
6. **Settings becomes an overlay, not a tab-stack entry.** This fixes M0 review item M-1: it can no longer be duplicated, and switching tabs closes it.
7. **Back on Today's root calls `moveTaskToBack(true)`** instead of `finish()`. This fixes M0 review item M-9: the activity and ViewModel survive, like the system default.
8. **Dependency Analysis keeps unused dependencies at "warn".** Feature modules are still mostly empty; this is revisited in M1c.

## Review Focus

1. **Unknown stored value.** A preference value the app no longer recognizes (a renamed enum after an upgrade or downgrade, or a restored backup) must fall back to the default without crashing. Pinned in Task 4 (`userPreferences_unknownStoredValue_fallsBackToDefault`).
2. **First launch.** With no DataStore file, the app shows System theme with the Default palette. Pinned in Task 4 (`userPreferences_noFile_emitsDefaults`).
3. **Choices apply and stick.** A theme or palette choice applies immediately and survives an app restart. Pinned in Task 4 (`setPalette_persistsAcrossNewDataStoreInstance`) and checked on the emulator in Task 5.
4. **TalkBack.** The Appearance options announce as radio buttons with their selected state. Pinned in Task 5 (`appearanceOptions_exposeRadioRoleAndSelection`).
5. **Status-bar contrast.** Status-bar icons follow the app's resolved theme, not the system's. With the app forced to Light while the system is Dark, the icons must stay dark on the light bar. Checked on the emulator in Task 5, Step 9.

## File Structure

```
core/model/.../core/model/ColorPalette.kt              enum DEFAULT, COLOR_VISION_SAFE, HIGH_CONTRAST
core/model/.../core/model/UserPreferences.kt           data class (themeMode, palette)
core/designsystem/.../theme/Contrast.kt                WCAG luminance + contrast ratio
core/designsystem/.../theme/DataColors.kt              data class + LocalDataColors
core/designsystem/.../theme/PaletteTokens.kt           the six instances' hex values (only file with Color literals for palettes)
core/designsystem/.../theme/Palettes.kt                colorSchemeFor(), dataColorsFor(), ContrastRequirement
core/designsystem/.../theme/Type.kt                    InterFontFamily, GetFitTypography
core/designsystem/.../theme/Shape.kt                   GetFitShapes
core/designsystem/.../theme/Theme.kt                   GetFitTheme(themeMode, palette) + object GetFitTheme
core/designsystem/.../icon/GetFitIcons.kt              @DrawableRes ids for Material Symbols
core/designsystem/src/main/res/font/inter_*.ttf        regular, medium, semibold, bold
core/designsystem/src/main/res/drawable/ic_*.xml       Material Symbols Outlined 24px
core/designsystem/src/main/assets/licenses/inter-OFL.txt
core/designsystem/src/test/.../theme/{ContrastTest,PaletteContrastTest,TypographyAndShapeTest,ThemeProvisionTest}.kt
core/domain/.../core/domain/UserPreferencesRepository.kt
core/datastore/.../core/datastore/UserPreferencesDataSource.kt
core/datastore/src/test/.../UserPreferencesDataSourceTest.kt
core/data/.../core/data/preferences/DefaultUserPreferencesRepository.kt
core/testing/.../core/testing/{FakeUserPreferencesRepository,MainDispatcherRule}.kt
app/.../di/PreferencesModule.kt                         DataStore provider + @Binds
feature/settings/.../{SettingsViewModel,SettingsScreen(rewritten),SettingsEntry}.kt
feature/settings/src/test/.../{SettingsViewModelTest,SettingsScreenSemanticsTest}.kt
app/.../navigation/TopLevelBackStack.kt                 + overlay
app/src/main/res/values{,-night}/{themes,colors}.xml    DayNight launch theme (M0 M-7)
detekt-rules/                                           new JVM module: NoColorLiteralOutsideDesignSystem
```

---

### Task 1: Contrast math, `DataColors` and the palette matrix

**Files:**
- Create: `core/model/src/main/kotlin/com/jrussell/fitness/core/model/ColorPalette.kt`, and in `core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/`: `Contrast.kt`, `DataColors.kt`, `PaletteTokens.kt`, `Palettes.kt`
- Delete: `core/designsystem/.../theme/Color.kt` (the M0 stub; `LightColors`/`DarkColors` are replaced)
- Modify: `core/designsystem/.../theme/Theme.kt` (use `colorSchemeFor(ColorPalette.DEFAULT, dark)` so it still compiles; full theme in Task 2), `docs/initial_design.md` (record the three user decisions above in the decision register and close the accent open question)
- Test: `core/designsystem/src/test/.../theme/ContrastTest.kt`, `PaletteContrastTest.kt`

**Interfaces:**
- Produces:
  - `enum class ColorPalette { DEFAULT, COLOR_VISION_SAFE, HIGH_CONTRAST }` (in `com.jrussell.fitness.core.model`)
  - `fun relativeLuminance(color: Color): Double` and `fun contrastRatio(foreground: Color, background: Color): Double`
  - `@Immutable data class DataColors(seriesPalette: List<Color>, macroProtein, macroCarbohydrate, macroFat, targetMet, targetUnder, targetOver, deltaPositive, deltaNegative, chartGridLine, chartAxisLabel, projectionLine, actualLine: Color)`. The field list is exactly the spec's.
  - `val LocalDataColors: ProvidableCompositionLocal<DataColors>`, created with `staticCompositionLocalOf` and defaulting to `dataColorsFor(DEFAULT, dark = false)`.
  - `fun colorSchemeFor(palette: ColorPalette, dark: Boolean): ColorScheme` and `fun dataColorsFor(palette: ColorPalette, dark: Boolean): DataColors`
  - `data class ContrastRequirement(val text: Double, val graphic: Double)` and `fun contrastRequirementFor(palette: ColorPalette): ContrastRequirement`. This returns 7.0/4.5 for `HIGH_CONTRAST` and 4.5/3.0 for the others.

- [ ] **Step 1: Create branch** `git checkout -b m1a-design-system`.

- [ ] **Step 2: Write `ContrastTest`** (JUnit 4, AssertK `isCloseTo(expected, 0.01)`):

```kotlin
@Test fun blackOnWhite_is21() { assertThat(contrastRatio(Color.Black, Color.White)).isCloseTo(21.0, 0.01) }
@Test fun sameColor_is1() { assertThat(contrastRatio(Color(0xFF6941C6), Color(0xFF6941C6))).isCloseTo(1.0, 0.01) }
@Test fun isSymmetric() { assertThat(contrastRatio(Color(0xFF777777), Color.White)).isCloseTo(contrastRatio(Color.White, Color(0xFF777777)), 0.0001) }
@Test fun gray777OnWhite_is4_48() { assertThat(contrastRatio(Color(0xFF777777), Color.White)).isCloseTo(4.48, 0.01) }
@Test fun violetOnDefaultLightBackground_is6_18() { assertThat(contrastRatio(Color(0xFF6941C6), Color(0xFFF7F7F8))).isCloseTo(6.18, 0.01) }
```

- [ ] **Step 3: Write `PaletteContrastTest`.** It is parameterized (`@RunWith(Parameterized::class)`) over all six `(ColorPalette, dark)` pairs, and each test's failure message names the palette, mode and token.
  - `textTokens_meetTextContrast`: checks `onBackground`, `onSurface` and `onSurfaceVariant` against both `background` and `surfaceContainer`; `primary` against `background`; `onPrimary` against `primary`; `onSecondaryContainer` against `secondaryContainer`; and `chartAxisLabel` against `background`. Each must be at least `contrastRequirementFor(palette).text`.
  - `graphicTokens_meetGraphicContrast`: checks every `seriesPalette` entry, the three macro colors, the three target colors, both delta colors, `projectionLine`, `actualLine` and `outline` against both `background` and `surfaceContainer`. Each must be at least `.graphic`. `chartGridLine` is exempt because it is decorative.
  - `seriesPalette_hasSixDistinctColors`: `seriesPalette.size == 6`, with 6 distinct values.

- [ ] **Step 4: Run** `./gradlew :core:designsystem:testDebugUnitTest`. Expected: FAIL to compile (unresolved `contrastRatio`, `ColorPalette`).

- [ ] **Step 5: Implement.** Luminance follows WCAG 2.x: sRGB channel ≤ 0.03928 → `c / 12.92`, otherwise `((c + 0.055) / 1.055)^2.4`; L = 0.2126 R + 0.7152 G + 0.0722 B. The ratio is `(L1 + 0.05) / (L2 + 0.05)` with L1 the lighter. `PaletteTokens.kt` holds these values; every pair has been verified to pass:

  **Neutrals and accent** (`ColorScheme` mapping):
  - `background` = `surface`
  - `surfaceContainer` covers all `surfaceContainer*` and `surfaceVariant`
  - `onBackground` = `onSurface`
  - `onSurfaceVariant` is the muted text color
  - `outlineVariant` is the hairline
  - `primary` is the accent
  - `primaryContainer` = `secondaryContainer`; `onPrimaryContainer` = `onSecondaryContainer` = `onBackground`
  - `error` = `targetOver`; `onError` = `onPrimary`

  | Instance | background | surfaceContainer | onBackground | onSurfaceVariant | outlineVariant | outline | primary | onPrimary | secondaryContainer |
  |---|---|---|---|---|---|---|---|---|---|
  | Default & CVS light | F7F7F8 | EFEFF2 | 1B1B1E | 5F5F68 | E2E2E6 | 85858F | 6941C6 | FFFFFF | E0DAF0 |
  | Default & CVS dark | 111113 | 1B1B1F | EDEDEF | A3A3AD | 2A2A2F | 6E6E78 | C3B1FF | 111113 | 433E55 |
  | High contrast light | FFFFFF | F2F2F4 | 000000 | 3A3A40 | CFCFD6 | 5A5A62 | 4B2A9E | FFFFFF | E2DDF0 |
  | High contrast dark | 000000 | 141416 | FFFFFF | CFCFD6 | 3A3A40 | A0A0AA | D9CCFF | 000000 | 3D3947 |

  **DataColors:**
  - `chartAxisLabel` = that instance's `onSurfaceVariant`
  - `chartGridLine` = its `outlineVariant`
  - `actualLine` = its `onBackground`
  - `seriesPalette[0]` and `projectionLine` = its `primary`, except in CVS, which uses the values below

  | Instance | series 1–5 | protein / carbohydrate / fat | met / under / over | delta + / − |
  |---|---|---|---|---|
  | Default light | 0F8B8D, BD7C03, D1477A, 3A7BD5, 6B8E23 | 0F8B8D / BD7C03 / D1477A | 1E7B34 / C77700 / B42318 | 1E7B34 / B42318 |
  | Default dark | 4CC9C0, F2B33D, F27CA6, 7FB0FF, A8C66C | 4CC9C0 / F2B33D / F27CA6 | 6FCF8A / F5B547 / FF8A80 | 6FCF8A / FF8A80 |
  | CVS light (series 0–5) | 0072B2, D55E00, 009B71, C76C9F, B87F00, 1B90D2 | 009B71 / 0072B2 / D55E00 | 0072B2 / B87F00 / B44F00 (darkened in final review: doubles as `error` text) | 0072B2 / D55E00 |
  | CVS dark (series 0–5) | 3D9BD6, E36F1A, 009E73, CC79A7, E69F00, 56B4E9 | 009E73 / 3D9BD6 / E36F1A | 3D9BD6 / E69F00 / E36F1A | 3D9BD6 / E36F1A |
  | High contrast light | 0D7B7D, 986303, C83269, 2B6DC8, 5A771D | 0D7B7D / 986303 / C83269 | 1E7B34 / 9E5F00 / 9D1F15 (darkened in final review) | 1E7B34 / B42318 |
  | High contrast dark | same as Default dark | same as Default dark | same as Default dark | same as Default dark |

  CVS `projectionLine`: C76C9F (light) / CC79A7 (dark).

- [ ] **Step 6: Run the tests again.** Expected: PASS. That's 5 contrast tests plus 3 tests × 6 instances.

- [ ] **Step 7: Record the decisions in `docs/initial_design.md`.** Add decision register row 37: "Typeface Inter; default accent violet #6941C6 / #C3B1FF; M1 split M1a–M1d". In the palette matrix, replace "chosen during M1" with "violet". Remove the accent bullet from Open questions.

- [ ] **Step 8: Commit** `feat(designsystem): WCAG contrast math, DataColors and six-instance palette matrix`.

---

### Task 2: Inter typography, shapes and the full `GetFitTheme`

**Files:**
- Create: `core/designsystem/src/main/res/font/inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`, `inter_bold.ttf`, `core/designsystem/src/main/assets/licenses/inter-OFL.txt`, `theme/Type.kt`, `theme/Shape.kt`
- Modify: `theme/Theme.kt`, `core/designsystem/build.gradle.kts` (test deps: `robolectric`, Compose `ui-test-junit4`, `ui-test-manifest` debug)
- Test: `theme/TypographyAndShapeTest.kt`, `theme/ThemeProvisionTest.kt` (Robolectric)

**Interfaces:**
- Consumes: Task 1 `colorSchemeFor`, `dataColorsFor`, `LocalDataColors`, `ColorPalette`
- Produces:
  - `val InterFontFamily: FontFamily`
  - `val GetFitTypography: Typography`
  - `val GetFitShapes: Shapes`
  - `@Composable fun GetFitTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, palette: ColorPalette = ColorPalette.DEFAULT, content: @Composable () -> Unit)`
  - `object GetFitTheme { val dataColors: DataColors }`, a `@Composable @ReadOnlyComposable` getter that reads `LocalDataColors.current`

- [ ] **Step 1: Get the fonts.** Download `https://github.com/rsms/inter/releases/download/v4.1/Inter-4.1.zip` into the scratchpad. Copy `extras/ttf/Inter-{Regular,Medium,SemiBold,Bold}.ttf` to `res/font/inter_{regular,medium,semibold,bold}.ttf` and `LICENSE.txt` to `assets/licenses/inter-OFL.txt`.

- [ ] **Step 2: Write `TypographyAndShapeTest`** (plain JUnit 4):

```kotlin
@Test fun everyTypographyStyle_usesInterWithTabularFigures() {
    allStyles(GetFitTypography).forEach { (name: String, style: TextStyle) ->
        assertThat(style.fontFamily, name).isEqualTo(InterFontFamily)
        assertThat(style.fontFeatureSettings, name).isEqualTo("tnum")
    }
}
@Test fun everyShapeCorner_isBetween2And4Dp() { /* extraSmall..extraLarge: each RoundedCornerShape corner size in [2.dp, 4.dp] */ }
```

`allStyles` is a test helper listing all 15 Material 3 styles by name.

- [ ] **Step 3: Write `ThemeProvisionTest`** (Robolectric, `createComposeRule()`):

```kotlin
@Test fun highContrastDark_providesMatchingSchemeAndDataColors() {
    var scheme: ColorScheme? = null; var data: DataColors? = null
    rule.setContent { GetFitTheme(ThemeMode.DARK, ColorPalette.HIGH_CONTRAST) { scheme = MaterialTheme.colorScheme; data = GetFitTheme.dataColors } }
    assertThat(scheme).isEqualTo(colorSchemeFor(ColorPalette.HIGH_CONTRAST, dark = true))  // compare background + primary
    assertThat(data).isEqualTo(dataColorsFor(ColorPalette.HIGH_CONTRAST, dark = true))
}
@Test fun lightMode_ignoresSystemDark() { /* ThemeMode.LIGHT under Robolectric qualifiers "night" → background == F7F7F8 */ }
```

- [ ] **Step 4: Run** `./gradlew :core:designsystem:testDebugUnitTest`. Expected: FAIL (unresolved `GetFitTypography`, `GetFitShapes`, new `GetFitTheme` parameters).

- [ ] **Step 5: Implement.**
  - **Typography:** take Material 3's default `Typography()` and copy every style with `fontFamily = InterFontFamily` and `fontFeatureSettings = "tnum"`. For the dense scale, override sizes and line heights: `titleLarge` 20/26 SemiBold, `titleMedium` 16/22 SemiBold, `titleSmall` 14/20 Medium, `bodyLarge` 15/22, `bodyMedium` 14/20, `bodySmall` 12/16, `labelLarge` 14/20 Medium, `labelMedium` 12/16 Medium, `labelSmall` 11/16 Medium.
  - **Shapes:** `extraSmall` 2 dp, `small` 3 dp, and 4 dp for the rest.
  - **Theme:** `GetFitTheme` resolves dark with `themeMode.resolveDark(isSystemInDarkTheme())`, then provides `LocalDataColors` and `MaterialTheme(colorScheme, GetFitTypography, GetFitShapes)`.

- [ ] **Step 6: Run the tests again.** Expected: PASS.

- [ ] **Step 7: Commit** `feat(designsystem): bundled Inter with tabular figures, 2–4 dp shapes, palette-aware GetFitTheme`.

---

### Task 3: Material Symbols icons

**Files:**
- Create: `core/designsystem/src/main/res/drawable/ic_{today,restaurant,fitness_center,monitor_weight,monitoring}{,_fill}.xml`, `ic_settings.xml`, `ic_arrow_back.xml`, `ic_check.xml`, and `core/designsystem/.../icon/GetFitIcons.kt`
- Modify: `app/.../navigation/TopLevelDestination.kt`, `app/.../ui/GetFitApp.kt`, `app/build.gradle.kts` and `gradle/libs.versions.toml` (remove `material-icons-core`)
- Test: `core/designsystem/src/test/.../icon/GetFitIconsTest.kt` (Robolectric)

**Interfaces:**
- Produces: `object GetFitIcons`, whose `@DrawableRes` properties are `Today`, `TodaySelected`, `Nutrition`, `NutritionSelected`, `Training`, `TrainingSelected`, `Body`, `BodySelected`, `Trends`, `TrendsSelected`, `Settings`, `Back` and `Check`. `TopLevelDestination` gains `@DrawableRes icon: Int` and `@DrawableRes selectedIcon: Int` in place of `ImageVector`.

- [ ] **Step 1: Download** each icon from `https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<name>/materialsymbolsoutlined/<name>_24px.xml` (selected variants: `<name>_fill1_24px.xml`). The mapping is: Today = `today`, Nutrition = `restaurant`, Training = `fitness_center`, Body = `monitor_weight`, Trends = `monitoring`, plus `settings`, `arrow_back` and `check`. In each file, remove `android:tint="?attr/colorControlNormal"`, because Compose's `Icon` applies `LocalContentColor`.

- [ ] **Step 2: Write `GetFitIconsTest`:** `everyIcon_inflatesAsVectorDrawable`. For each `GetFitIcons` property, `ContextCompat.getDrawable(context, id)` is a `VectorDrawable` with a 24 dp intrinsic size.

- [ ] **Step 3: Run it.** Expected: FAIL (unresolved `GetFitIcons`).

- [ ] **Step 4: Implement `GetFitIcons`** and switch the bottom bar to `painterResource(if (selected) destination.selectedIcon else destination.icon)`. Remove the `material-icons-core` dependency and catalog entry.

- [ ] **Step 5: Run** `./gradlew :core:designsystem:testDebugUnitTest :app:assembleDebug buildHealth`. Expected: PASS, and buildHealth reports no `material-icons-core` advice.

- [ ] **Step 6: Commit** `feat(designsystem): Material Symbols icons replace material-icons-core placeholders`.

---

### Task 4: Persisted UI preferences (theme mode and palette)

**Files:**
- Create:
  - `core/model/.../UserPreferences.kt`
  - `core/domain/.../UserPreferencesRepository.kt`
  - `core/datastore/.../UserPreferencesDataSource.kt`
  - `core/data/.../preferences/DefaultUserPreferencesRepository.kt`
  - `core/testing/.../FakeUserPreferencesRepository.kt`
  - `core/testing/.../MainDispatcherRule.kt`
  - `app/.../di/PreferencesModule.kt`
- Modify:
  - `gradle/libs.versions.toml` (`datastore = "1.2.1"`, library `androidx-datastore-preferences`)
  - `core/datastore/build.gradle.kts` (`api(datastore-preferences)`, `implementation(projects.core.model)`, test: `kotlinx-coroutines-test`, `turbine`)
  - `core/data/build.gradle.kts`
  - `core/testing/build.gradle.kts` (`api(kotlinx-coroutines-test)`, `api(junit4)`, `implementation(projects.core.domain)`)
- Test: `core/datastore/src/test/.../UserPreferencesDataSourceTest.kt`

**Interfaces:**
- Produces:
  - `data class UserPreferences(val themeMode: ThemeMode, val palette: ColorPalette)`, with `companion object { val DEFAULT: UserPreferences = UserPreferences(ThemeMode.SYSTEM, ColorPalette.DEFAULT) }`
  - `interface UserPreferencesRepository { val userPreferences: Flow<UserPreferences>; suspend fun setThemeMode(themeMode: ThemeMode); suspend fun setPalette(palette: ColorPalette) }`
  - `class UserPreferencesDataSource(dataStore: DataStore<Preferences>)`, which has the same three members. It stores enum `name`s under the keys `theme_mode` and `color_palette`.
  - `class DefaultUserPreferencesRepository @Inject constructor(dataSource: UserPreferencesDataSource) : UserPreferencesRepository`, which delegates to the data source.
  - `class FakeUserPreferencesRepository : UserPreferencesRepository`, backed by a `MutableStateFlow`.
  - `class MainDispatcherRule(dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher`
  - In `:app`, `PreferencesModule` provides a `@Singleton DataStore<Preferences>` from `PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("user_preferences") }`, plus `@Provides UserPreferencesDataSource` and `@Binds UserPreferencesRepository`.

- [ ] **Step 1: Write `UserPreferencesDataSourceTest`.** It runs on JUnit 4 with `runTest` and Turbine, and builds each DataStore with `PreferenceDataStoreFactory.create(scope = backgroundScope) { tempFolder.newFile("prefs.preferences_pb") }`. The tests:

```kotlin
@Test fun userPreferences_noFile_emitsDefaults()                    // first item == UserPreferences.DEFAULT
@Test fun setThemeMode_emitsUpdatedValue()                          // DARK → themeMode == DARK, palette unchanged
@Test fun setPalette_persistsAcrossNewDataStoreInstance()           // write HIGH_CONTRAST, close scope, new DataStore on same file → HIGH_CONTRAST
@Test fun userPreferences_unknownStoredValue_fallsBackToDefault()   // edit raw key color_palette = "SEPIA" → palette == DEFAULT, no exception
```

- [ ] **Step 2: Run** `./gradlew :core:datastore:testDebugUnitTest`. Expected: FAIL (unresolved `UserPreferencesDataSource`).

- [ ] **Step 3: Implement.** Map stored values with `runCatching { enumValueOf<T>(name) }.getOrDefault(default)` or `entries.firstOrNull`. Add the repository, the fake, `MainDispatcherRule` and the `:app` Hilt module.

- [ ] **Step 4: Run the tests again** with `./gradlew :core:datastore:testDebugUnitTest :app:assembleDebug`. Expected: PASS, 4 tests.

- [ ] **Step 5: Commit** `feat: persisted theme mode and palette preferences (DataStore)`.

---

### Task 5: Settings → Appearance, live theming, and shell fixes

**Files:**
- Create:
  - `feature/settings/.../SettingsViewModel.kt`, plus tests `SettingsViewModelTest.kt` and `SettingsScreenSemanticsTest.kt`
  - `app/src/main/res/values/themes.xml`, `values/colors.xml`, `values-night/colors.xml`
  - `architecture-tests` test `ViewModelDependencyRulesTest.kt`
- Modify:
  - `feature/settings/.../SettingsScreen.kt`, `SettingsEntry.kt`, `res/values/strings.xml`
  - `app/.../MainViewModel.kt`, `MainActivity.kt`, `ui/GetFitApp.kt`, `navigation/TopLevelBackStack.kt`, plus its test
  - `app/src/main/AndroidManifest.xml`
  - `build-logic/.../AndroidFeatureConventionPlugin.kt`. Add test deps: `projects.core.testing`, `turbine`, `kotlinx-coroutines-test`, `robolectric`, Compose `ui-test-junit4`, and `ui-test-manifest` as debugImplementation.

**Interfaces:**
- Consumes: Task 4 `UserPreferencesRepository`, `UserPreferences`, `FakeUserPreferencesRepository`, `MainDispatcherRule`; Task 2 `GetFitTheme`; Task 3 `GetFitIcons`
- Produces:
  - `@HiltViewModel class SettingsViewModel @Inject constructor(repository: UserPreferencesRepository)`, with `val uiState: StateFlow<UserPreferences>` (via `stateIn(WhileSubscribed(5_000), UserPreferences.DEFAULT)`), `fun onThemeModeSelected(themeMode: ThemeMode)` and `fun onPaletteSelected(palette: ColorPalette)`
  - `MainViewModel` gains `val userPreferences: StateFlow<UserPreferences>`
  - `TopLevelBackStack` gains `val overlay: K?` (Compose state), `fun openOverlay(key: K)` and `fun closeOverlay()`. `backStack` = flattened tab stacks + overlay.

- [ ] **Step 1: Write the failing tests.**
  - `TopLevelBackStackTest` additions:
    - `openOverlay_appendsOnTop`
    - `openOverlay_twice_doesNotDuplicate`
    - `switchTo_closesOverlay` (Body + Settings → switchTo Nutrition → `[Today, Nutrition]`)
    - `removeLast_closesOverlayFirst` (returns true, overlay null, tab stack unchanged)
  - `SettingsViewModelTest`: `uiState_reflectsRepository`, `onPaletteSelected_writesRepository`, `onThemeModeSelected_writesRepository`
  - `SettingsScreenSemanticsTest` (Robolectric): `appearanceOptions_exposeRadioRoleAndSelection`. Each of the 6 options has `Role.RadioButton`, and only `HIGH_CONTRAST` and `DARK` are `isSelected()` for that state.
  - `ViewModelDependencyRulesTest` (Konsist, M0 review M-6): `featureViewModels_dependOnlyOnDomainInterfaces`. Every primary-constructor parameter type of a class named `*ViewModel` in `feature/*` resolves to an interface in package `com.jrussell.fitness.core.domain`. Prove it fails once by temporarily adding a parameter of type `UserPreferencesDataSource`. Then remove it.

- [ ] **Step 2: Run** `./gradlew :app:testDebugUnitTest :feature:settings:testDebugUnitTest :architecture-tests:test`. Expected: FAIL.

- [ ] **Step 3: Implement.**
  - **Settings screen:** two labeled groups, "Theme" (System, Light, Dark) and "Colors" (Default, Color-vision-safe, High contrast). Each row is `Modifier.selectable(selected, role = Role.RadioButton)`, contains a `RadioButton(onClick = null)`, and is at least 48 dp tall. Hairline dividers separate the groups. Every string is a resource. The stateless `AppearanceSettings(state, onThemeModeSelected, onPaletteSelected)` composable carries `@PreviewLightDark` and `@PreviewFontScale` previews, as the spec requires.
  - **MainActivity:** collects `userPreferences` with `collectAsStateWithLifecycle` and passes them into `GetFitTheme`. A `DisposableEffect(resolvedDark)` calls `enableEdgeToEdge(SystemBarStyle.auto(transparent, transparent) { resolvedDark })` so the status-bar icons follow the app theme.
  - **Shell:** opens Settings with `openOverlay(SettingsKey)` and shows the Back icon while the overlay is open. `onExit` calls `moveTaskToBack(true)`. Pass `tonalElevation = 0.dp` to `NavigationBar` and give it `containerColor = surfaceContainer`. Add a hairline `HorizontalDivider` (`outlineVariant`) above the bar.
  - **Launch theme:** add `Theme.GetFit` with parent `android:Theme.DeviceDefault.DayNight`, `windowActionBar=false`, `windowNoTitle=true`, and `windowBackground=@color/window_background`. That color is `#F7F7F8`, with `#111113` in `values-night`. Set it as the application theme. This fixes M0 review item M-7.

- [ ] **Step 4: Run** `./gradlew check buildHealth assembleDebug`. Expected: PASS.

- [ ] **Step 5: Emulator check (debug build).** Expected outcomes:
  - Settings opens from any tab, with the back arrow in the top bar. Switching tabs closes it.
  - Choosing Dark, then High contrast, restyles the app immediately.
  - Force-stop and relaunch keeps both choices.
  - With the system in dark mode and the app set to Light, the cold-start window is light with no dark flash, and the status-bar icons are dark.
  - At font scale 2.0 (`adb shell settings put system font_scale 2.0`), Settings rows and tab labels are not clipped. Reset the scale to 1.0 afterwards.
  - TalkBack on a palette row announces "Selected/Not selected, radio button".

- [ ] **Step 6: Commit** `feat(settings): Appearance (theme, palette) with live theming; Settings overlay; DayNight launch theme`.

---

### Task 6: Custom detekt rule — no `Color(` outside `:core:designsystem`

**Files:**
- Create: `detekt-rules/build.gradle.kts`, `detekt-rules/src/main/kotlin/com/jrussell/fitness/detekt/{GetFitRuleSetProvider,NoColorLiteralOutsideDesignSystem}.kt`, `detekt-rules/src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider`, `detekt-rules/src/test/kotlin/.../NoColorLiteralOutsideDesignSystemTest.kt`
- Modify: `settings.gradle.kts`, `gradle/libs.versions.toml` (`detekt-api`, `detekt-test`, both on `version.ref = "detekt"`), `build-logic/.../QualityConventionPlugin.kt` (add `detektPlugins(project(":detekt-rules"))` to every module except `:detekt-rules`), `config/detekt/detekt.yml` (`getfit: NoColorLiteralOutsideDesignSystem: active: true`), `build-logic/.../AndroidLibraryComposeConventionPlugin.kt` (M0 review item M-8: `val composeBom: Dependency`)

**Interfaces:**
- Produces: rule set id `getfit` and rule `NoColorLiteralOutsideDesignSystem`. The rule reports any call expression whose callee is `Color` in a file whose package does not start with `com.jrussell.fitness.core.designsystem`.

- [ ] **Step 1: Write the tests** with detekt-test's `lint(code)` helper (JUnit 5; the module applies `app.jvm.library`):

```kotlin
@Test fun colorCall_inFeaturePackage_isReported()          // package ...feature.today; val c = Color(0xFF000000) → 1 finding
@Test fun colorCall_inDesignSystemPackage_isAllowed()      // package ...core.designsystem.theme → 0 findings
@Test fun colorConstant_isAllowed()                         // Color.Unspecified / MaterialTheme.colorScheme.primary → 0 findings
```

- [ ] **Step 2: Run** `./gradlew :detekt-rules:test`. Expected: FAIL (unresolved rule).

- [ ] **Step 3: Implement** the rule against detekt 2.0.0-alpha.6's `Rule` API (`visitCallExpression`, `report(Finding(Entity.from(expression), message))`). Read the exact constructor signatures from the `detekt-api` jar. The message is "Colors come from GetFitTheme / MaterialTheme; define new colors in :core:designsystem."

- [ ] **Step 4: Run** `./gradlew :detekt-rules:test check`. Expected: PASS, and `check` reports no findings in the codebase.

- [ ] **Step 5: Prove it on real code.** Temporarily add `val bad: Color = Color(0xFF000000)` to `feature/today/.../TodayScreen.kt`. `./gradlew :feature:today:detekt` should FAIL, naming `NoColorLiteralOutsideDesignSystem`. Revert the line.

- [ ] **Step 6: Commit** `build: custom detekt rule forbidding Color literals outside :core:designsystem`.

---

### Task 7: Exit check and merge

- [ ] **Step 1:** Run `./gradlew check buildHealth assembleDebug assembleRelease`. Expected: PASS.
- [ ] **Step 2:** Install the signed release on the emulator. Repeat the Task 5 Step 5 checks on the release build; this catches R8 stripping DataStore, Hilt or font resources. Logcat should show 0 crashes.
- [ ] **Step 3:** Final whole-branch review, then merge `m1a-design-system` into `master` (no tag; `v0.1.0` comes after M1d).
