# Navigation and Home: design

**Status:** draft for owner approval, 2026-10-17.

**Amends:** `docs/initial_design.md` decision #30 (navigation layout), the module inventory, "Charts", "Interaction patterns", `MetricTarget`, and the M1c and M6 milestone text.

**Mockups:** `.superpowers/brainstorm/nav-mockups-v1.html` and `chart-builder-v1.html` (local, git-ignored). The owner reviewed both and approved their direction.

## Intent

The original layout has five tabs: Today, Nutrition, Training, Body, Trends. Today and Trends cut across the other three, so the owner wants navigation organized around what they do:

> "I would want a sort of home page dashboard that gives me a summary of where I'm at for the day … I would want this to be a read only view, basically where I'm at at a glance. Then, I'd want to be able to either work with food, exercise, measurements, or whatever else I need to do by switching to that tab."

There are two kinds of screens:
- **Home** is for *looking*: a read-only summary, plus charts that span domains.
- **Nutrition, Training and Body** are for *doing*: logging and editing, plus charts about that one domain.

## Decisions made by the owner

1. **Four bottom tabs:** Home, Nutrition, Training, Body. Settings stays behind the gear icon in the top bar.
2. **Home is read-only.** It summarizes and never takes data entry.
3. **Home defaults to today.** The user can pick a range instead (presets plus "from a start date"). Home then summarizes from that date to today.
4. **Nutrition counts only what has been eaten.** Planned food appears as a projection ("if you finish today's plan"), never in the main figures.
5. **Range summaries follow how each target is measured:**
   - a metric with a **daily** target shows a **per-day average** ("2,900 kcal/day on average");
   - a metric with a **period** target shows a **tally** ("21 workouts in 3 weeks, against 7 per week").
6. **Cross-domain charts are viewed, created and pinned from Home.** The owner wants:
   - many metrics on one chart, scaled to line up ("weight, sleep, calories, water all in one graph … normalized somehow");
   - bar charts as well as lines;
   - a two-metric comparison for cause-and-effect questions ("sleep vs deadlift volume so I can see how when my sleep is lower my deadlift suffers").
7. **Single-domain charts live in their own tab** (a Trends view inside Nutrition, Training or Body) and can be pinned to Home from there.
8. **The chart builder's three views** (from the mockups) are approved:
   - *Overlay, scaled*;
   - *Overlay, real units*;
   - *Compare two* (dots, with an optional one-day shift, or the same pair over time).

9. **Charts on Today show the last 7 days.** Other ranges apply to charts as chosen.
10. **Projection plans live in the Body tab**, as a Plan view beside Log and Trends.

## Goals

- Opening the app shows where you stand today in one glance, with no taps needed.
- Any combination of metrics from any domain can be put on one chart and pinned to Home.
- Range views answer "how did I do over this period?" in the units that matter for each metric.
- Each work tab is self-contained: log, review history, see its own charts.

## Non-goals

- **Editing from Home.** Tapping a card navigates to the tab that owns it, and editing happens there.
- **A free-form dashboard layout** (drag-to-resize grids). Home's card order is fixed; pinned items appear in a pinned section in the order they were pinned and can be reordered there.
- **Statistics beyond a plain-language correlation strength.** No p-values, regressions or forecasts in the chart builder. Weight projection stays the projection engine's job.

## Anti-goals (this would be a failure even if it worked)

- **Home turns into a data-entry screen.**
- **A chart misleads:**
  - a scaled chart that hides the real values;
  - a correlation presented as cause;
  - series told apart by color alone (the spec's redundant-encoding rules apply to every chart).
- **A range figure that is technically correct but useless**, such as total calories over three weeks.

## Navigation shell

- **Tabs:** Home, Nutrition, Training, Body. App launch opens Home; "back at the root of a non-Home tab returns to Home" replaces today's "returns to Today". `TopLevelBackStack` keeps its per-tab histories and the Settings overlay unchanged.
- **Home cards:** tapping a card switches to that tab:
  - Nutrition card → Nutrition;
  - Training card → Training;
  - Body card → Body;
  - Energy card → Body.
  - *(Builder's call: the Energy target may change once energy accounting has its own home in M5.)*
- **Work tabs:** each starts with a **Log / Trends** switch under the top bar. Body adds a third view, **Plan** (projection plans, from M5; owner decision, 2026-10-17). The switch is state inside the tab's screen, not a navigation entry. The selection persists per tab across tab switches and process recreation.
- **Chart builder:** opens as a pushed screen on the stack of the tab it was opened from. That is Home for cross-domain charts and the owning tab for single-domain charts.

## Home

### Cards (top to bottom)

| Card | Today | Range |
| --- | --- | --- |
| **Nutrition** | Energy eaten against target and remaining; protein, carbs and fat against targets; direct water against its goal. A secondary line: "with today's plan: N kcal" | Per-day averages, and "target met on X of Y complete days" |
| **Training** | Sessions completed today (name, duration) and planned but not yet completed | Sessions completed against the target scaled to the range; skipped count |
| **Energy out** | Expenditure so far today (active, and total from the active energy mode) | Average daily energy balance |
| **Body** | Latest weight with the weight trend (lb or kg per week; see "Weight trend" below); progress against the active projection plan once plans exist (M5); then the user's pinned measurements (latest value each) | Each pinned measurement: start → now, with rate per week |
| **Pinned charts** | Each pinned chart, drawn over Home's range | Same |

Cards whose data does not exist yet (e.g. Nutrition before M4) are hidden, not shown empty.

### Date range

- **Presets:** Today (the default), 7 days, 30 days, and "From…", which opens a date picker for the start date. The end is always today.
- **Days follow the user's day start** (`localDateFor`).
- **The choice is not persisted.** Home opens on Today every time *(builder's call: it matches "today by default")*.
- **Charts on Today show the last 7 days** (owner decision, 2026-10-17). A one-day chart would show almost nothing, so pinned charts on Home, and charts in a Trends view set to Today, draw the 7 days ending today. Every other range applies to charts as chosen.

### Range summary rules

Every metric gets a **summary rule**, derived from its descriptor and its target:

| Rule | Used for | Range shows |
| --- | --- | --- |
| `AVERAGE_PER_DAY` | Metrics with a daily target, or daily-sum metrics with no target (energy, macros, nutrients, water, sleep, steps) | The mean of the daily values. Nutrition metrics average over **COMPLETE days only** (the existing rule: incomplete days never feed aggregates). Also shows how many of those days met the target |
| `TALLY` | Metrics with a period target, and count-like metrics (sessions, hard sets) | The total over the range, against the target scaled to the range length: weekly target × days ÷ 7 |
| `CHANGE` | Body measurements (`dailyAggregation` LAST) | First value in the range → last value in the range, and the rate per week: the slope of the least-squares line through the values in the range (0 with fewer than two dates). With no value inside the range, the latest value at both ends and a rate of 0 |

`MetricDescriptor` gains `summaryRule: SummaryRule`. Targets gain a period (see Data model), and a metric with a `WEEK` target uses `TALLY` whatever its default rule.

## Chart builder

One builder, used in two places:
- **from Home:** every metric is available;
- **from a tab's Trends view:** only that domain's metrics.

It edits a `ChartSpec` and renders it with the single chart component in `:core:ui` (Vico underneath, per decision #32). Every chart offers "View as table" and a generated spoken summary, as the spec already requires.

### Views

**Overlay, scaled**
- Any number of series.
- Each series is mapped to 0–1 over its own minimum and maximum within the visible range. A flat series sits at the middle.
- Dragging along the chart shows each series' real value and unit for that day.
- Each series can be drawn as a line or as bars.

**Overlay, real units**
- Series are drawn in their own units on at most two axes (one unit per axis).
- Adding a series with a third unit switches the chart to *scaled*, and a note explains why.

**Compare two**
- Choose an x metric and a y metric. The x metric can be shifted by one day: "the day before" (lag −1), "the same day" (0) or "the day after" (+1).
- **Dots view:** one dot per date where both have values, plus a least-squares line.
- **Over time view:** the same pair as two scaled lines.
- **Strength statement:** a plain-language sentence based on the Pearson r of the paired values. The wording bands are the builder's call:
  - |r| < 0.1: "no clear link";
  - < 0.3: "a weak link";
  - < 0.5: "a moderate link";
  - otherwise: "a strong link".

  The direction is stated ("more X went with more/less Y"). The sentence always ends "a link, not proof of cause". With fewer than 7 paired days it says "not enough data yet".

### Series styling

- Colors come from `DataColors.seriesPalette` in order.
- Each series also gets a distinct dash pattern or marker and a direct end label, per the redundant-encoding table.

### Saving and pinning

- **"Pin to Home"** saves the chart and pins it. The user can also save without pinning.
- **Pinned charts** can be unpinned and reordered from Home's pinned section (long-press a chart → unpin, move up, move down, edit).
- **Single-domain charts** saved in a tab appear in that tab's Trends view, and can additionally be pinned to Home.
- **The date range is never stored with a chart.** A chart always follows the range of the screen showing it: Home's range, or the tab Trends view's own range control (same presets as Home), with Today drawn as the last 7 days.

## Data model

All new tables are user-owned (standard columns, owner-scoped DAOs, stamped writes).

| Table | Columns | Keys |
| --- | --- | --- |
| `SavedChart` | `name`, `scope` (`HOME` or a domain: `NUTRITION`, `TRAINING`, `BODY`), `specJson` (serialized `ChartSpec`), `pinnedToHome`, `homeOrder` (nullable) | `(ownerId, scope)`; `(ownerId, pinnedToHome, homeOrder)` |
| `HomePinnedMeasurement` | `measurementTypeId`, `displayOrder` | `(ownerId, measurementTypeId)` unique; `(ownerId, displayOrder)` |

`ChartSpec` (in `:core:domain`, serialized with kotlinx.serialization) **extends** the original spec's `ChartSpec` rather than replacing it. Transforms and target reference lines are kept. It holds:
- `view` (`OVERLAY_SCALED`, `OVERLAY_UNITS`, `COMPARE`);
- `series: List<SeriesSpec>`, where `SeriesSpec` is:
  - `metricId`;
  - `style` (`LINE`, `BAR`);
  - `lagDays: Int` (default 0);
  - `transforms: List<Transform>` (default empty; the original spec's EMA, rolling mean, week-over-week delta, cumulative sum and percent-of-target);
- `compareDisplay` (`DOTS`, `OVER_TIME`), for COMPARE only;
- `showTargetLines: Boolean` (default true): reference lines from `MetricTarget` where a series has one.

Every field has a default, and decoding ignores unknown keys, so charts saved by an older build always load and fields added later never break them. The builder UI exposes transforms gradually: trend smoothing (EMA) and rolling mean arrive with the builder in M2, and the rest in the M6 polish. *(Builder's call.)*

Unknown metric IDs (e.g. a deleted custom measurement type) render as a "metric no longer available" placeholder rather than failing.

**`MetricTarget` change:** add `period` (`DAY`, `WEEK`), defaulting to `DAY`. Weekly targets (sessions per week, hard sets per muscle per week) use `WEEK`.

Pinned measurements and saved charts live in `user.db`, not DataStore, because they are the user's setup. They are backed up and will sync. *(Builder's call; DataStore keeps only UI preferences such as theme.)*

## Modules

| Change | Detail |
| --- | --- |
| `:feature:today` → `:feature:home` | Home screen, its ViewModel, and the Home-scoped chart builder screen |
| `:feature:trends` | Removed. Cross-domain charting moves to `:feature:home`. **Projection plans** move to `:feature:body` as its **Plan** view (owner decision, 2026-10-17) |
| `:core:ui` | Gains the reusable chart-builder UI (stateless composables over a `ChartSpec`), so `:feature:home` and each work tab's Trends view share it without depending on each other |
| `:core:domain` | `ChartSpec`, `SummaryRule`, `SavedChartRepository`, `HomePinRepository` |

The dependency rules are unchanged. Features still never depend on each other; the shared builder UI is in `:core:ui` and the data is behind `:core:domain` interfaces.

## Milestone impact (builder's proposal)

- **M1c:**
  - the Body tab gets Log / Trends, with manual weight entry, history, and the weight trend chart with a table view;
  - Home ships with the range control and the **Body card** (weight and trend), the only domain with data;
  - the tab bar becomes Home, Nutrition, Training, Body.
  - The M1 exit check is unchanged: "enter weights → see the weight chart (weigh-in line and dotted trend line) and table → export, wipe, restore, identical data".
- **M2** (when sleep and heart data first make cross-domain charts meaningful): the full chart builder (all three views), saved and pinned charts, pinned measurements, and Body Trends charts for the new metrics.
- **M3 / M4:** the Training card, Training Trends, the Nutrition card with projection, Nutrition Trends, and weekly targets.
- **M5:** Body's Plan view (projection plans, simulator, plan-versus-actual chart), and plan progress on Home's Body card.
- **M6:** "Today dashboard, Trends metric explorer" becomes a polish and accessibility pass over Home and the builder.

### Scope of the next plan

- This document is the design for the whole change. The **M1c plan** implements only the M1c slice above, plus the spec amendments below.
- Tables, targets and rules a later milestone needs (`SavedChart`, `HomePinnedMeasurement`, `MetricTarget.period`, `SummaryRule`) are added in the milestone that first uses them. After `v0.1.0` that means a numbered `CREATE TABLE` migration, which is the normal process, not something to avoid by creating tables early. *(Builder's call.)*

## Spec amendments on approval

`docs/initial_design.md`:
- **Decision #30:** "Four bottom tabs: Home, Nutrition, Training, Body; Settings in the top bar (amended 2026-10-17)".
- **New decision #38:** the content of this document, by reference.
- **Module inventory:** the `:feature:home` and `:feature:trends` rows, and the `:core:ui` responsibility.
- **"Interaction patterns" → Navigation:** the new tab set.
- **"Charts":** the builder views and saved/pinned charts.
- **`MetricTarget`:** add `period`. Add the `SavedChart` and `HomePinnedMeasurement` tables.
- **"Energy accounting → Daily budget":** "The Today screen shows" becomes "Home's Energy card shows".
- **Milestones M1c and M6:** the text above.

## Amendment 2026-10-18: Weight trend

Decided with the user after trying the first weight chart on the emulator. Showing the smoothed "trend weight" (decision #28) next to a real weight confused more than it helped ("Trend 214.1 lb" when the scale said 212.4).

- **What the user sees as "trend" is a rate**, in lb or kg per week: the slope of the least-squares line through the weigh-ins in a range, with each weigh-in placed by its date. With fewer than two days of weigh-ins it is 0.0 (no "not enough data" message).
- **The weight chart** draws a solid line through each day's weigh-in, with a dot on each, as the main series, and the trend as a dotted straight line from the first to the last weigh-in in the range. The table lists only the weigh-ins, with the spoken summary (which includes the rate) above it.
- **Where the rate is measured:** Body's latest-weight card uses the last 30 days. Home's Weight card uses Home's range, with Today using the last 7 days like Today's charts.
- **Trend weight (decision #28) stays** as an internal metric (`BODY_TREND_WEIGHT`) for the M5 adaptive expenditure estimate and PAL calibration. It is no longer displayed.

### Every chart, and the trend-line switch (same day)

- **The rule holds for every chart, not just weight.** The shared chart component draws each series as its values connected by a solid line with a dot on each point (the main line), and derives the trend itself: a dotted least-squares line through that series' points, in the same color, drawn under the main line. Any rate a chart reports ("per week" in the spoken summary and table) is that line's slope. Screens pass only recorded values; they never build a trend series.
- **Each chart can hide its trend line.** Next to "View as table", every chart has "Hide trend line" / "Show trend line" (on whenever the chart is first shown; kept across rotation). Hiding it removes the dotted line; the summary's rate stays. A chart whose points fall on one date has no trend and no button. (Revised the same day: a Settings › Charts switch was tried first and replaced by this per-chart button.)
