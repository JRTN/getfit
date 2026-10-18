# 5. Design system

[`:core:designsystem`](../../core/designsystem/) owns every color, the type scale, shapes and icons. Screens never invent their own: they read from the theme. That is what lets one setting restyle the whole app, including into color-vision-safe and high-contrast variants.

## The look

- **Flat and dense, a data tool rather than a toy.** No tonal elevation (`tonalElevation = 0.dp`); surfaces are separated by hairline dividers ([`GetFitApp`](../../app/src/main/kotlin/com/jrussell/fitness/ui/GetFitApp.kt)'s bottom bar is the first example).
- **Nearly square corners:** 2–4 dp ([`Shape.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Shape.kt)), where Material's defaults are 4–28 dp.
- **Neutral grays with one accent:** violet, `#6941C6` in light and `#C3B1FF` in dark (decision #37, picked from mockups).
- **No dynamic (wallpaper) color.** Chart colors must mean the same thing on every phone.

## The palette matrix

The user picks a theme mode (System, Light, Dark) and a palette (Default, Color-vision-safe, High contrast), which gives **six instances** (three palettes × light/dark).

| File | What it holds |
| --- | --- |
| [`PaletteTokens.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/PaletteTokens.kt) | The raw hex values of all six instances. It is the only file with palette color literals. Tokens use `null` to mean "use the accent", so each instance defines its accent once |
| [`Palettes.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Palettes.kt) | `colorSchemeFor(palette, dark)` builds the Material `ColorScheme`; `dataColorsFor(palette, dark)` builds the app's own data colors |
| [`DataColors.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/DataColors.kt) | Colors Material has no slot for: six chart series, protein/carbs/fat, target met/under/over, positive/negative deltas, grid, axis label, projection and actual lines |
| [`Theme.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Theme.kt) | `GetFitTheme(themeMode, palette) { … }` provides both, plus typography and shapes; `ThemeMode.resolveDark()` resolves System using the device setting |

In a composable, colors come from two places:

```kotlin
MaterialTheme.colorScheme.primary          // UI chrome: text, buttons, surfaces
GetFitTheme.dataColors.macroProtein        // data: charts, macros, targets
```

**The color-vision-safe palette** keeps the violet UI accent and switches only the data colors to Okabe–Ito hues, adjusted in lightness to pass contrast. **High contrast** uses near-black and near-white neutrals with stricter thresholds.

## Contrast is tested, not eyeballed

[`Contrast.kt`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/Contrast.kt) implements WCAG relative luminance and contrast ratio. [`ContrastRequirement`](../../core/designsystem/src/main/kotlin/com/jrussell/fitness/core/designsystem/theme/ContrastRequirement.kt) sets the thresholds:

| Palette | Text | Graphics (chart lines, icons, outlines) |
| --- | --- | --- |
| Default, Color-vision-safe | ≥ 4.5 : 1 | ≥ 3 : 1 |
| High contrast | ≥ 7 : 1 | ≥ 4.5 : 1 |

[`PaletteContrastTest`](../../core/designsystem/src/test/kotlin/com/jrussell/fitness/core/designsystem/theme/PaletteContrastTest.kt) runs over all six instances. It checks every text token and every graphic token against both `background` and `surfaceContainer` (bars and cards sit on the latter), and it requires six distinct chart-series colors. **Change a hex value and the test tells you whether it still passes.** (In M1a it caught the `error` color failing as text in two palettes.)

## Color is never the only signal

Some users cannot tell certain colors apart. Every status shown in color also carries a second cue: an icon, a sign, a label or a pattern. The spec's "Redundant encoding" table lists the pairings, and later screens must follow it.

### Charts

[`MetricChart`](../../core/ui/src/main/kotlin/com/jrussell/fitness/core/ui/chart/MetricChart.kt) in `:core:ui` is the only chart component; features never call Vico. It encodes everything at least twice:

| What | Shown by |
| --- | --- |
| A series' recorded values vs its trend | Shape: a **solid line with a dot on each point** vs a **dotted line** (same palette color), plus the legend's labels and samples |
| Two series on one chart | Palette color *and* their own legend labels ("Weight", "Weight trend") |
| The numbers themselves | "View as table": the recorded values as rows, one TalkBack announcement per row |
| The overall shape | The spoken summary, the chart's content description: "Body weight, 30 days, 216.0 to 212.4 pounds, decreasing 0.9 pounds per week" |
| Change direction on cards | A sign ("−1.4 lb/week"), never color alone |

Every chart also follows one rule for trends: the dotted line is the least-squares best fit through the series, and every "per week" rate the app shows is its slope. "Hide trend line" next to "View as table" turns the dotted line off for that chart.

## No color literals outside the design system

The custom detekt rule [`NoColorLiteralOutsideDesignSystem`](../../detekt-rules/src/main/kotlin/com/jrussell/fitness/detekt/NoColorLiteralOutsideDesignSystem.kt) fails the build on any of these outside `com.jrussell.fitness.core.designsystem`:
- `Color(0xFF…)`;
- `Color.Red`-style constants and `Color.hsl`/`hsv`;
- `android.graphics.Color.parseColor`/`rgb`;
- an aliased import of `Color`.

`Color.Unspecified` and `Color.Transparent` are allowed. A new color is added to the tokens, gets a name in the scheme or in `DataColors`, and is covered by the contrast test.

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
- **Radio groups** (Settings options, range chips, the Log/Trends switch) expose their roles and selected state to TalkBack, verified by each feature's semantics tests (e.g. [`SettingsScreenSemanticsTest`](../../feature/settings/src/test/kotlin/com/jrussell/fitness/feature/settings/SettingsScreenSemanticsTest.kt), [`RangeChipsTest`](../../core/ui/src/test/kotlin/com/jrussell/fitness/core/ui/range/RangeChipsTest.kt)).
- **Cards and list rows read as one sentence** ("Latest weight 212.4 pounds, today, −1.2 pounds per week since Oct 11"), and destructive row actions are also offered as TalkBack custom actions (Delete on each weigh-in).
- **Field errors** are set as the field's error semantics, so TalkBack announces "Enter a weight between 45 and 881 lb".
- **Large fonts:** at 200% system font, chips and chart buttons wrap onto another line instead of clipping. Tab labels cap their font scale at 1.3× so the labels fit without breaking words; screen content always gets the full scale.
- **Strings** are always resources (no hard-coded user-visible text).

## Theme mode and the system bars

- The user's light/dark choice is applied through `UiModeManager.setApplicationNightMode` (in [`MainActivity`](../../app/src/main/kotlin/com/jrussell/fitness/MainActivity.kt)). Android then shows the right colors from the cold-start window onward.
- The launch theme in `app/src/main/res/values{,-night}/themes.xml` is a DayNight theme whose window background matches the Default palette.
- Status-bar and navigation-bar icons follow the app's resolved theme, not the system's, so forcing Light on a Dark phone still gives dark icons on light bars.

Next: [Build and quality](06-build-and-quality.md).
