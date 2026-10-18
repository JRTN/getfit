# GetFit

**A local-first Android app for nutrition, training and body tracking, in one place.**

> **Status:** `v0.1.0`, the first usable release (M1 complete).
> - **Built:** the design system, theming and settings, the local data foundation, weight logging with a Home summary and trend charts, and three backup layers (Auto Backup, daily snapshots to a folder of my choosing, JSON and database export and restore).
> - **Next:** M2, Body and Health Connect.
>
> The roadmap is [below](#roadmap).

Fitness is a big deal to me. At my heaviest, I weighed 330 lbs at 6'2" tall, and I was horrendously out of shape. After successfully losing 120 lbs down to 210 lbs simply by tracking what I ate, I found that I was still not healthy; I was weak; I had no endurance; and I still felt bad.

Then, I discovered what fitness is. Fitness is a lifestyle. It is eating well, exercising enough, getting quality sleep, and holding yourself accountable. As a backend software engineer, I love data. I use data about my fitness to identify where my accountability is slipping, and where I can make adjustments to better reach my goals.

For a long time I've tracked my health across several apps. Cronometer for what I eat, Strong for workouts, and an Excel to work out where my weight is heading. Each does its own job reasonably well, but each has quirks that have frustrated me over time. Building a week of meals or a new training block means tapping in one item at a time. And none of them share data: my food log has never met my training log, so the questions I care about most -- how what I eat lines up with how I train, and what both mean for my weight -- are left for me to answer by hand. Worse, the data they collect and allow for export is limited and takes different shapes.

As AI tools become better and better, I saw an opportunity to solve my own problem, learn a new style of development, and grow as a software engineer. I wanted to see how far I could get building a real, daily-use app with an AI coding agent doing most of the implementation, while I acted as product owner, reviewer and occasional pair programmer. 

That is where **GetFit** comes in. This is an app I intend to use every day to monitor my fitness and an experiment in AI-driven development that holds itself to the standards of hand-written code; or, rather, the standards I wish I had time to hold myself to.

-John

---

## Why this exists

**One app, not several.** Food, training and body measurements are one system: energy in, work done, weight out. GetFit keeps them in one database, so any metric can be charted against any other (protein against training volume, sleep against the next day's performance), and a weight projection can use what was actually eaten and lifted.

**Bulk work should be easy.** Logging one item is rarely the problem. Adding a week of meals, a dozen exercises, or a whole training block is. Multi-select, batch edits and templates are planned as first-class features, not afterthoughts.

**I own the data.** Everything lives on the phone in SQLite, with no account and no subscription. Three backup layers are part of the first usable release: Android Auto Backup, scheduled database snapshots to a folder of my choosing, and versioned JSON export. The schema is designed so that sync to a self-hosted server can be added later without migrating existing data.

**A real project to learn from.** A toy app would not show whether AI-driven development holds up. GetFit has real data integrity requirements, real algorithms and real accessibility needs, so it does.

## AI-driven development

Most of GetFit's code is written by an AI coding agent ([Claude Code](https://claude.com/claude-code)). My role is mainly product owner and reviewer: I decide what gets built, make the product calls, and review every step. At times I'm closer to a pair programmer, steering details. And I'm partly a learner, using the project to sharpen my own Android and engineering practice by reading what gets produced.

The working agreement is simple: **AI-written code has to be at least as trustworthy as code I would write by hand, and I have to be able to understand all of it.** These practices make that hold:

1. **Spec before code.** [`docs/initial_design.md`](docs/initial_design.md) is the authority: a numbered decision register, the schema, the algorithms and the milestones. Product decisions are mine and are recorded there with dates.
2. **A written plan for every milestone.** Before any code, the agent writes a plan ([`docs/superpowers/plans/`](docs/superpowers/plans/)) giving the exact files, interfaces and test names for each task. It also lists every judgment call it made where the spec is silent, with what each costs if wrong, and the failure modes most likely to hurt a user. I review the plan before work starts.
3. **Test-first, always.** Every behavior starts as a failing test that is run and seen to fail before the implementation is written. A test that has never failed has proven nothing.
4. **Small steps with a human checkpoint.** Work proceeds one task at a time on a short-lived branch, and I review a summary of each task before the next begins. Every decision the agent makes on my behalf is written into a ledger and reported to me, so nothing is decided in secret.
5. **An independent final review.** Before a branch merges, a separate AI reviewer with fresh context reads the entire diff against the plan, the spec and the recorded decisions. Serious findings are fixed by first writing a test that reproduces them; minor ones are logged and carried into the next plan.
6. **Machines enforce the rules.** Conventions that matter become compiler errors, lint rules or tests, so neither the agent nor I have to remember them.
7. **Docs that keep up.** Every merge to `master` updates the [developer guide](docs/developer/README.md), so I can stop at any point, read how the code fits together, and click through it.

Commits written with the agent carry a `Co-Authored-By` trailer.

## Planned features

- **Nutrition:** search a bundled USDA catalog that keeps every nutrient it provides; a diary with meal slots, planned and consumed entries, multi-select actions and quick-add; recipes; food edits that never silently rewrite history; versioned targets; hydration; CSV import of existing history.
- **Training:** a live workout logger that saves every set the moment it is entered; templates; an exercise library with custom exercises; previous performance, PRs and estimated 1RM; a rest timer; CSV import of existing history.
- **Body:** manual measurements, plus Health Connect import (weight, body fat, heart rate, HRV, SpO2, sleep, steps) with de-duplication and source priority.
- **Home and charts:** a read-only summary of where you are today, or over any range; charts of any metric against any other, each with a best-fit trend; plus energy-balance estimates and a weight projection engine that reports whether a target is realistic.
- **Accessible by default:** color-vision-safe and high-contrast palettes checked against WCAG ratios by tests; color is never the only signal; designed for TalkBack and 200% font size.

The full specification, including every decision and why it was made, is in [`docs/initial_design.md`](docs/initial_design.md).

## How it is built

| | |
| --- | --- |
| **Language and UI** | Kotlin (explicit style), Jetpack Compose, Material 3, Navigation 3 |
| **Data** | Room (SQLite) for user data, DataStore for UI preferences, kotlinx-datetime, `kotlin.uuid` |
| **Architecture** | About 24 Gradle modules in five layers; Hilt; pure-Kotlin domain modules ready for Kotlin Multiplatform |
| **Quality** | Test-first (JUnit, Robolectric, Turbine, AssertK); detekt with custom rules; ktlint; Konsist architecture tests; Android Lint; Dependency Analysis |
| **Platform** | Android 14+ (minSdk 34, target 37) |

A few design choices worth a look:

- **Every write is stamped.** Each database transaction increments a global revision counter and records the time and the device on every row it touches, so sync can be added later without a migration. See [`StampedWriter`](core/data/src/main/kotlin/com/jrussell/fitness/core/data/write/StampedWriter.kt).
- **Rules are enforced by machines, not memory.** Features cannot reach storage, because Gradle won't compile it. Color literals, direct dispatchers and destructive migrations are banned by custom detekt rules. Every palette's contrast is a unit test.
- **Time is handled carefully.** A configurable day start (log at 01:30, count it toward yesterday) that daylight saving time cannot break. See [`localDateFor`](core/domain/src/main/kotlin/com/jrussell/fitness/core/domain/DayAssignment.kt).

**For developers:** start with the [developer guide](docs/developer/README.md). It walks through the architecture, follows the running app through the code, and explains the [stack and why each piece was chosen](docs/developer/stack.md).

## Roadmap

Versioning is semantic: `0.1.0` is the first usable release, each new capability bumps the minor version, and `1.0.0` is the original design complete.

| Milestone | Scope | State |
| --- | --- | --- |
| M0 | Skeleton: modules, build logic, navigation shell, CI, signing | Done (`v0.0.1`) |
| M1a | Design system: palettes and contrast tests, typography, icons, appearance settings | Done |
| M1b | Data foundation: `user.db`, stamped writes, owner scoping, day assignment, sync seam | Done |
| M1c | Four-tab layout with a read-only Home, weight entry, metric registry, charts with a best-fit trend and table view | Done |
| M1d | Backup: Auto Backup, scheduled snapshots, JSON and database export and restore | Done (**`v0.1.0`**) |
| M2 | Body and Health Connect | Next |
| M3 | Nutrition, including saved meals | |
| M4 | Training, including training blocks | |
| M5 | Energy accounting, weight projection and the cross-domain chart builder on Home | |
| M6 | Daily-driver release: polish and accessibility pass over Home and the chart builder | |

## Building

Requirements: Android Studio (current stable) and the API 37 SDK platform.

```bash
./gradlew installDebug        # build and install the debug app
./gradlew check buildHealth   # tests, static analysis, architecture and dependency checks
```

The debug app installs as "GetFit (Debug)" beside any release install, with its own data. Details, including release signing, are in [Getting started](docs/developer/01-getting-started.md).
