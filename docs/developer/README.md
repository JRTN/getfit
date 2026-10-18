# GetFit developer guide

This guide is a walkthrough for a developer joining GetFit. It explains what each part of the codebase does, where the important classes live, and why things are built the way they are. Every chapter links straight to source files, so you can read with the code open beside you.

> **Where things stand:** written after milestone **M1d** (backup and restore) merged to `master`, completing M1 (`v0.1.0`). The app has its design system, four tabs (a read-only Home, then Nutrition, Training and Body), Settings, the local database, weight logging with a Home summary and a trend chart, and three backup layers: Auto Backup, a daily snapshot to a folder of your choosing, and manual export and restore. Nutrition and Training are still empty. The guide is updated with every merge to `master`.

## How this guide relates to the other docs

| Document | What it is | When to read it |
| --- | --- | --- |
| [`docs/initial_design.md`](../initial_design.md) | The **spec**: what GetFit must do and the decisions behind it (the decision register, schema, algorithms) | When you need to know what the app is meant to do |
| `docs/developer/` (this guide) | The **walkthrough**: how the code that exists today is put together | When you want to understand or change the code |
| [`docs/developer/stack.md`](stack.md) | The **stack**: every library and tool, why it was picked, what was rejected | When you wonder "why X and not Y?" |
| [`docs/superpowers/plans/`](../superpowers/plans/) | The **plans**: task-by-task build instructions for each milestone, kept as a record | When you want the history of a piece of code |
| [`docs/backlog.md`](../backlog.md) | The **backlog**: known work no plan has taken on yet, grouped by milestone | Before writing a plan, or when you wonder whether a gap is known |
| [`docs/reviews/`](../reviews/) | **Reviews** of the whole app and design, with the owner's decisions | When you want to know why the design changed course |

The spec describes the destination; this guide describes the road built so far. Where they differ, the code follows the spec, and the guide says what is not built yet.

## Reading order

1. [Getting started](01-getting-started.md): toolchain, building, running, and inspecting the app on an emulator.
2. [Architecture](02-architecture.md): the module layers, the dependency rules, and how they are enforced.
3. [Runtime tour](03-runtime-tour.md): follow the app from process start to the first frame, then through a preference change and a database write. **Start here if you only read one chapter.**
4. [Data layer](04-data-layer.md): `user.db`, revision stamping, owner scoping, day assignment, seeding, and the sync seam.
5. [Design system](05-design-system.md): palettes, contrast tests, typography, icons, and theming.
6. [Build and quality](06-build-and-quality.md): the convention plugins, versioning, signing, and every automated check.
7. [Conventions and testing](07-conventions-and-testing.md): code style, error handling, coroutines, and how tests are written.
8. [Development workflow](08-development-workflow.md): how features go from design to plan to code to `master`.

Then read [the stack](stack.md) whenever a library choice raises a question.

## A five-minute orientation

- **What it is:** a local-first Android app that brings nutrition logging, strength training and weight projection into one place, replacing several separate apps that never shared data. All data lives on the phone.
- **Shape:** a single-activity Jetpack Compose app. It is split into about 24 Gradle modules in five layers: `:app` → `:feature:*` → `:core:*` UI → `:core:*` data → pure Kotlin.
- **Data:** Room (`user.db`) holds the user's data. Every write runs in one transaction that stamps a revision number, a timestamp and a device ID, so a future sync can be added without migrating anything. Preferences that only affect the UI live in DataStore.
- **Rules enforced by machines, not memory:** layer boundaries (Gradle and Konsist), no color literals outside the design system, no direct `Dispatchers.IO`, no destructive migrations (custom detekt rules), WCAG contrast for every palette (unit tests), and unused or misdeclared dependencies (Dependency Analysis).
- **Style:** explicit Kotlin (explicit types, block bodies), KDoc that explains *why*, and test-first development.

## Keeping this guide current

Every merge to `master` includes a docs step: new modules, classes and decisions get added here in the same change, so the guide always describes `master`. If you find a mismatch, the code is right and the guide is a bug. Fix it.
