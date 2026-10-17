# 8. Development workflow

GetFit is built with AI-driven development: an AI coding agent writes most of the code, and the human owner decides what gets built, reviews, and steers. The workflow exists so that the result is as trustworthy as hand-written code. Every step leaves a written record, and every claim is backed by a test or a command's output.

## From idea to `master`

```
spec (docs/initial_design.md)
  └─▶ milestone plan (docs/superpowers/plans/<date>-<name>.md)
        └─▶ branch, then task by task: failing test → code → green → commit
              └─▶ final whole-branch review by a fresh reviewer → fixes (each test-first)
                    └─▶ full suite green → fast-forward merge to master → developer docs updated
```

1. **The spec** ([`docs/initial_design.md`](../initial_design.md)) is the authority. It holds the decision register (numbered decisions with dates), the schema, the algorithms and the milestone list. When a decision changes, the spec is amended with a date.
2. **A plan** turns one milestone (or part of one) into tasks. Each plan states:
   - its global constraints;
   - the calls it makes where the spec is silent, each with its cost if wrong;
   - a **Review Focus** list of the failure modes most likely to bite a user;
   - exact files, interfaces and test names per task.
3. **Execution** happens task by task on a short-lived branch.
   - Each task starts with failing tests, which are run and seen to fail. Then the code makes them pass, the module checks run, and the task is committed.
   - A ledger records every decision taken on the plan's behalf ("rulings"), with its cost if wrong.
   - The owner reviews a summary after each task before the next starts.
4. **Final review.** A fresh reviewer, a separate AI agent on the most capable model that has not seen the work, reads the whole branch diff against the plan, the spec, the rulings and the Review Focus list. Critical and Important findings are fixed in one pass, each starting from a test that reproduces it. Minor findings are recorded and carried into a later plan.
5. **Merge.** The full suite (`check buildHealth assembleDebug assembleRelease`) must pass, then the branch is fast-forward merged into `master`. Releases are tagged; see [Build and quality](06-build-and-quality.md#versioning).
6. **Docs.** This guide is updated in the same change, so it always describes `master`.

## History so far

| Milestone | Plan | Delivered |
| --- | --- | --- |
| **M0 — Skeleton** (`v0.0.1`) | [m0-skeleton](../superpowers/plans/2026-10-16-m0-skeleton.md) | All modules, convention plugins, version catalog, Hilt, Navigation 3 with five empty tabs, crash log, CI workflow, release signing, quality tooling |
| **M1a — Design system** | [m1a-design-system](../superpowers/plans/2026-10-16-m1a-design-system.md) | Palette matrix with contrast tests, Inter, Material Symbols, Settings → Appearance on DataStore, Settings as an overlay, the color-literal detekt rule |
| **M1b — Data foundation** | [m1b-data-foundation](../superpowers/plans/2026-10-17-m1b-data-foundation.md) | `user.db` v1, revision-stamped writes, owner scoping, typed UUID IDs, day assignment, measurement and day-start repositories, the sync seam, injected dispatchers, the dispatcher and migration detekt rules |
| M1c — Weight, metrics, charts | *next* | Metric registry, chart component with table toggle, manual weight entry, trend weight |
| M1d — Backup | | Auto Backup rules, scheduled SQLite export, JSON export/import; `v0.1.0` |

Each plan's "Calls made in this plan" section, and the decision register in the spec, explain why things are the way they are. When code surprises you, look there first.

## Working on the code yourself

- Branch from `master` with a short-lived branch, keep commits small, and use conventional prefixes (`feat:`, `fix:`, `build:`, `docs:`).
- Write the test first. Run `./gradlew check buildHealth assembleDebug assembleRelease` before merging.
- If you change how something works, update this guide in the same change.
- The repository is on GitHub ([JRTN/getfit](https://github.com/JRTN/getfit), private for now). CI runs on every push; push `master` after merging.

Back to the [guide index](README.md), or on to [the stack](stack.md).
