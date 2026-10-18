# Backlog

Known work that no plan has picked up yet. Each plan reads this file when it is written, moves the items it takes on into its tasks, and adds the minor findings it defers. Items are grouped by the milestone expected to touch the same code. A milestone's plan may move an item to another milestone, saying so here.

**Sources:**
- **review §n:** `docs/reviews/2026-10-18-pre-v0.1.0-review.md`;
- **M1c:** deferred from the M1c final review (`docs/superpowers/plans/2026-10-18-m1c-weight-home-charts.md`, "Calls made during execution").

Milestone order (decision #39): M2 Body and Health Connect → M3 Nutrition → M4 Training → M5 Energy, projection and the chart builder → M6 Daily-driver release.

## Before writing the M2 plan (decisions that become stored data in M2)

- **One metric ID scheme.** Today the rules disagree: `body.<measurement type key>` versus `vital.heart_rate`, `activity.steps`. Saved charts and targets will store these IDs. Also decide whether user-defined types are identified by an immutable key or by type ID. (review §3.1)
- **Namespace user-defined measurement-type keys** (e.g. `custom.<slug>`), so a later built-in can't be shadowed by `findByKey`, which prefers the owner's row. (review §3.2)
- **Metric labels that aren't Android resources.** Replace `MetricDescriptor.displayNameResource: Int` with a label type: a resource key or literal text, resolved in `:core:ui`. Never display built-in names from the database (`MeasurementType.displayName` is English). (review §3.3)
- **Source priority, per day and per type, as one domain function** the metric providers share. Today `BodyMetricProvider` takes the day's last reading whatever its source. (review §3.4)
- **Health Connect aggregates.**
  - Re-aggregate the affected days when the change token reports records.
  - Give aggregate rows synthetic `externalId`s (`hr:2026-10-18`).
  - Decide where min, mean and max go.
  - Document this as a deliberate exception to "store raw observations".

  (review §3.5)
- **A top-bar pattern for pushed screens and the multi-select action bar.** The shell's single `TopAppBar` can't serve the `MeasurementType` editor or later pushed screens. (review §3.7)
- **An allow-list in `ViewModelDependencyRulesTest`** for `SavedStateHandle` or assisted navigation keys; an editor needs its row's ID. (review §3.8)
- **Query convention:** bounded date ranges and SQL aggregates instead of all-history flows recomputed in Kotlin. The `Measurement` table will hold every Health Connect type, so every import burst re-runs every observer. (review §4.6)
- **Deterministic UUIDv5 IDs for rows with a natural key** (one per date, per exercise, per measurement type) in every new table, so future sync and JSON merges reconcile by ID. (review §4.5a)

## M2: Body and Health Connect

- **Day-start setting UI.** Until it exists, a time picked without a date uses the calendar date rather than the logical day. (M1c; review §6.4)
- **Reviving tombstoned day-start rows.** When day-start settings can be deleted, revive the tombstoned row instead of inserting, because the unique `(ownerId, effectiveFrom)` index spans tombstones. (M1b)
- **Body Trends with more than one series:** distinct dash or marker per series and direct end labels; the legend wraps at 200% font size. (review §3.6)
- **Long ranges:** bucket "All" charts by week or month automatically once they span many months. (review §4.7)
- **Chart table:** rows read values without a unit in TalkBack, and `ChartTable` composes every row eagerly. (M1c)
- **Home Weight card:** it has no `onClickLabel` ("open Body") for TalkBack. (M1c)
- **Weigh-in editing:**
  - a failed save gives no feedback;
  - a weigh-in can't be edited;
  - deletes have no undo;
  - the delete confirmation is lost on rotation.

  (M1c; review §6.2)

## M3: Nutrition

- **`SummaryRule`** (nav spec) arrives with the Nutrition card. Move weight's rate onto it, so Home and Body stop computing summaries separately. (review §5.1)
- **Replacing `reference.db`:** "replaced wholesale" needs its own reset logic, because `createFromAsset` copies only once, and `NoDestructiveMigration` will flag a destructive fallback. (M1b, detekt gaps)
- **Cronometer import:** decide how per-entry rows become food versions without duplicates. (review §4.7)
- **Child tables and sync:** decide whether `FoodNutrient`, `ServingSize` and `RecipeIngredient` travel inside their parent's payload ("aggregate sync"). They have no `id`, owner or revision columns. (review §4.5b)

## M4: Training

- **Rest timer on Android 14 and later:** choose between a typed foreground service and a notification countdown with an inexact alarm. `shortService` times out after about 3 minutes, and exact alarms are denied by default. (review §4.3)
- **Resume an in-progress workout** after process death: the tab back stack isn't saved, so the user lands on Home. (review §4.4)
- **Strong import:** its CSV has no unit column, so ask for the unit. (review §4.7)
- **Training schema notes:** load conventions, `side`, WEIGHT_DISTANCE, a decimal `rir`, entered distance units, and source columns on sessions. See "Schema notes for later milestones" in the design doc.

## M5: Energy, projection and the chart builder

- **Solver:** "physically impossible" (required intake below 0) can't come out of bisection over `[0, 2 × E₀]`. Check whether fasting (I = 0) misses the target first. (review §4.7)
- **Chart builder encoding:** every series gets a distinct dash or marker and an end label, so color is never the only cue. (review §3.6)

## M6 and polish

- **Crash-log and log viewer in Settings,** with share or export. Logs are written (crash logs since M0, the file logger from M1d) but invisible. (review §6.3)
- **App icon:** GetFit has none yet.
- **Launch window and High contrast:** the launch window uses the Default palette background, so High contrast sees a brief tone shift on cold start. (M1c)
- **Back stack after process death:** returning lands on Home rather than where the user was. (review §6.4)
- **Restore counts don't match the preview.** The preview counts live entries ("4 entries"), but "Restored: 5 added" counts every merged row, including tombstones and the day-start setting. Count the same thing in both. (M1d drill)
- **The folder-unavailable message appears twice.** After a failed Back up now, "Can't reach the backup folder. Choose it again." shows as both the result line and the status line, so TalkBack reads it twice. (M1d drill)
- **Value and unit through a resource:** "212.4 lb" is built by string concatenation in `HomeScreen.kt`, not through a resource. (review §5.4)
- **Small cleanups:**
  - a formatting nit in `MetricChart`;
  - `trendLine` computed twice;
  - `TopLevelBackStack.backStack` unused by the UI.

  (M1c)

## Tooling

- **`NoDirectDispatchers` misses some forms:** the `Dispatchers.IO` import plus bare `IO`, aliases, and any file declaring package `di`. (M1b)
- **detekt 2.0.0-alpha.6 is a hard CI gate.** Pin it deliberately, and expect breakage on upgrades. (review §5.5)
- **Consolidate spec amendments.** Appended amendments in the nav spec should be merged into its body text, keeping a dated changelog. (review §7.2)

## Later (sync, multiple users)

- **Clock drift:** last-writer-wins on device wall-clock `updatedAt` can let a stale write win when clocks drift. Add a hybrid logical clock column with sync. (review §4.5c)
- **Per-owner preferences:** DataStore preferences are per install, not per owner. Revisit with multiple users. (review §4.7)
