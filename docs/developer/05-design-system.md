# 5. Design system

[`:core:designsystem`](../../core/designsystem/) owns every colour, the type scale, shapes and icons. Screens never invent their own: they read from the theme. That is what lets one setting restyle the whole app, including into colour-vision-safe and high-contrast variants.

## The look

- **Flat and dense, a data tool rather than a toy.** No tonal elevation (`tonalElevation = 0.dp`); surfaces are separated by hairline dividers ([`GetFitApp`](../../app/src/main/kotlin/com/jrussell/fitness/ui/GetFitApp.kt)'s bottom bar is the first example).
- **Nearly square corners:** 2–4 dp ([`Shape.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Shape.kt)), where Material's defaults are 4–28 dp.
- **Neutral greys with one accent:** violet, `#6941C6` in light and `#C3B1FF` in dark (decision #37, picked from mockups).
- **No dynamic (wallpaper) colour.** Chart colours must mean the same thing on every phone.

## The palette matrix

The user picks a theme mode (System, Light, Dark) and a palette (Default, Colour-vision-safe, High contrast), which gives **six instances** (three palettes × light/dark).

| File | What it holds |
| --- | --- |
| [`PaletteTokens.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/PaletteTokens.kt) | The raw hex values of all six instances. It is the only file with palette colour literals. Tokens use `null` to mean "use the accent", so each instance defines its accent once |
| [`Palettes.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Palettes.kt) | `colorSchemeFor(palette, dark)` builds the Material `ColorScheme`; `dataColorsFor(palette, dark)` builds the app's own data colours |
| [`DataColors.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/DataColors.kt) | Colours Material has no slot for: six chart series, protein/carbs/fat, target met/under/over, positive/negative deltas, grid, axis label, projection and actual lines |
| [`Theme.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Theme.kt) | `GetFitTheme(themeMode, palette) { … }` provides both, plus typography and shapes; `ThemeMode.resolveDark()` resolves System using the device setting |

In a composable, colours come from two places:

```kotlin
MaterialTheme.colorScheme.primary          // UI chrome: text, buttons, surfaces
GetFitTheme.dataColors.macroProtein        // data: charts, macros, targets
```

**The colour-vision-safe palette** keeps the violet UI accent and switches only the data colours to Okabe–Ito hues, adjusted in lightness to pass contrast. **High contrast** uses near-black and near-white neutrals with stricter thresholds.

## Contrast is tested, not eyeballed

[`Contrast.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Contrast.kt) implements WCAG relative luminance and contrast ratio. [`ContrastRequirement`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/ContrastRequirement.kt) sets the thresholds:

| Palette | Text | Graphics (chart lines, icons, outlines) |
| --- | --- | --- |
| Default, Colour-vision-safe | ≥ 4.5 : 1 | ≥ 3 : 1 |
| High contrast | ≥ 7 : 1 | ≥ 4.5 : 1 |

[`PaletteContrastTest`](../../core/designsystem/src/test/kotlin/com/jrussell/fitness/core/designsystem/theme/PaletteContrastTest.kt) runs over all six instances. It checks every text token and every graphic token against both `background` and `surfaceContainer` (bars and cards sit on the latter), and it requires six distinct chart-series colours. **Change a hex value and the test tells you whether it still passes.** (In M1a it caught the `error` colour failing as text in two palettes.)

## Colour is never the only signal

Some users cannot tell certain colours apart. Every status shown in colour also carries a second cue: an icon, a sign, a label or a pattern. The spec's "Redundant encoding" table lists the pairings, and later screens must follow it.

## No colour literals outside the design system

The custom detekt rule [`NoColorLiteralOutsideDesignSystem`](../../detekt-rules/src/main/kotlin/com/jrussell/fitness/detekt/NoColorLiteralOutsideDesignSystem.kt) fails the build on any of these outside `com.jrussell.fitness.core.designsystem`:
- `Color(0xFF…)`;
- `Color.Red`-style constants and `Color.hsl`/`hsv`;
- `android.graphics.Color.parseColor`/`rgb`;
- an aliased import of `Color`.

`Color.Unspecified` and `Color.Transparent` are allowed. A new colour is added to the tokens, gets a name in the scheme or in `DataColors`, and is covered by the contrast test.

## Typography

- **Inter 4.1**, bundled as static TTFs in `res/font/`, under the SIL Open Font License ([`assets/licenses/inter-OFL.txt`](../../core/designsystem/src/main/assets/licenses/inter-OFL.txt)).
- [`Type.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Type.kt) sets every Material text style to Inter with the OpenType feature `tnum` (**tabular figures**). Every digit has the same width, so numbers line up in columns and don't jitter as they change, which matters in a numbers-heavy app.

## Icons

- **Material Symbols (Outlined)**, imported as Android vector drawables in `res/drawable/ic_*.xml`. They are referenced through [`GetFitIcons`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/icon/GetFitIcons.kt), never by resource name in features.
- Tabs have an outlined icon and a filled `…Selected` variant.
- Directional icons (Back) are auto-mirrored for right-to-left languages. [`GetFitIconsTest`](../../core/designsystem/src/test/kotlin/com/jrussell/fitness/core/designsystem/icon/GetFitIconsTest.kt) inflates every icon and checks the mirroring flag.

**To add an icon:** download the Outlined 24 px Android drawable from Material Symbols, remove its `android:tint` attribute, add it to `res/drawable/`, expose it in `GetFitIcons` (and in `GetFitIcons.all`), and set `android:autoMirrored="true"` if it points in a direction.

## Accessibility built in so far

- **Touch targets** stay at 48 dp or larger (`minimumInteractiveComponentSize()`), even though rows look compact.
- **Settings radio groups** expose radio roles and selected state to TalkBack, which [`SettingsScreenSemanticsTest`](../../feature/settings/src/test/kotlin/com/jrussell/fitness/feature/settings/SettingsScreenSemanticsTest.kt) verifies.
- **Tab labels** cap their font scale at 1.3× so five labels fit at 200% system font without breaking words; screen content always gets the full scale.
- **Strings** are always resources (no hard-coded user-visible text).

## Theme mode and the system bars

- The user's light/dark choice is applied through `UiModeManager.setApplicationNightMode` (in [`MainActivity`](../../app/src/main/kotlin/com/jrussell/fitness/MainActivity.kt)). Android then shows the right colours from the cold-start window onward.
- The launch theme in `app/src/main/res/values{,-night}/themes.xml` is a DayNight theme whose window background matches the Default palette.
- Status-bar and navigation-bar icons follow the app's resolved theme, not the system's, so forcing Light on a Dark phone still gives dark icons on light bars.

Next: [Build and quality](06-build-and-quality.md).
