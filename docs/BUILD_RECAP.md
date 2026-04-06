# Stryker Mutation Testing Plugin — Build Recap

**Size:** FEATURE | **Risk:** MODERATE | **Slices:** 3 | **Tests:** 83 passing

## What We Built

A JetBrains IDE plugin that lets developers run StrykerJS mutation testing directly from the Project View context menu. Right-click a JS/TS file or folder, run Stryker, see console output in a dedicated tool window, and get survived mutants highlighted inline in the editor with warning annotations and gutter icons.

## Architecture

**Pattern:** Feature-Based Packages within `com.github.nicetester.stryker`

```
action/    → Context menu entry point (RunStrykerAction)
runner/    → Process execution + console output (StrykerRunner, StrykerConsole, StrykerToolWindowFactory)
model/     → Data classes + JSON parsing (MutationReport, ReportParser)
annotator/ → Editor annotations + gutter icons (MutantAnnotator, MutantGutterIconRenderer)
service/   → Shared state — mutation results (MutationResultService)
util/      → Path helpers (PathUtil)
```

**Dependency direction:** `action/ → runner/ → service/ ← annotator/`, both through `model/`. No cycles. `MutationResultService` is the single shared-state boundary.

## Files

### Production Code (12 files)

| File | Purpose |
|------|---------|
| `StrykerBundle.kt` | i18n message bundle |
| `action/RunStrykerAction.kt` | "Run Stryker" context menu action, file type filtering, mutate pattern building |
| `runner/StrykerRunner.kt` | Background task: spawns `npx stryker run`, captures output, parses report |
| `runner/StrykerConsole.kt` | Manages "Stryker Output" tool window and console view |
| `runner/StrykerToolWindowFactory.kt` | ToolWindowFactory for plugin.xml registration |
| `model/MutationReport.kt` | Data classes: `MutationReport`, `MutantResult`, `MutantLocation` |
| `model/ReportParser.kt` | Parses Stryker JSON report via Gson, filters to survived mutants |
| `annotator/MutantAnnotator.kt` | ExternalAnnotator + GutterIconRenderer for inline warnings |
| `service/MutationResultService.kt` | Project-scoped @Service: stores results, tracks run state |
| `util/PathUtil.kt` | Shared `computeRelativePath()` utility |
| `META-INF/plugin.xml` | Plugin descriptor: action, tool window, notification group, annotator |
| `messages/StrykerBundle.properties` | 14 user-facing message strings |

### Test Code (5 files, 83 tests)

| File | Tests | Coverage |
|------|-------|----------|
| `RunStrykerActionTest.kt` | 34 | File type filtering, action visibility, path computation, folder globs |
| `StrykerRunnerTest.kt` | 8 | Output summary parsing (survived/killed extraction) |
| `MutationResultServiceTest.kt` | 12 | Service registration, run state, result storage/retrieval/clearing |
| `ReportParserTest.kt` | 15 | JSON parsing, survived-only filter, multi-file, invalid JSON |
| `MutantAnnotatorTest.kt` | 14 | Tooltip formatting, gutter icon rendering, annotator lifecycle |

## How It Works

### Flow: Right-Click → Annotations

1. **User right-clicks** a JS/TS file or folder in Project View
2. **`RunStrykerAction.update()`** checks: is it a JS/TS file or directory? Show/hide the action.
3. **`RunStrykerAction.actionPerformed()`** checks concurrent run guard, verifies npx, builds mutate pattern
4. **`StrykerRunner.run()`** clears old results, runs `npx stryker run --mutate <pattern>` in background
5. **Console output** streams to "Stryker Output" tool window via `StrykerConsole`
6. **On success**, `parseAndStoreReport()` reads `reports/mutation/mutation.json`
7. **`ReportParser.parseJson()`** extracts survived mutants only
8. **`MutationResultService.setResults()`** stores results + triggers `DaemonCodeAnalyzer.restart()`
9. **`MutantAnnotator.collectInformation()`** picks up results for open files
10. **`MutantAnnotator.apply()`** creates WARNING annotations + gutter icons on affected lines

### Error Handling

| Error | Behavior |
|-------|----------|
| npx not found | Error notification: "Node.js/npx is required" |
| Non-zero exit code | Error notification with exit code |
| Missing stryker.conf | Stryker's own error output shown in console (not swallowed) |
| Report file missing | Warning notification: "Check Stryker config for JSON reporter" |
| Invalid JSON report | Error notification instead of crash |
| Concurrent run | Warning notification: "Stryker run already in progress" |

## Acceptance Criteria (21/21)

### Slice 1: Run Stryker on a Single File (9 ACs)
- [x] "Run Stryker" action in Project View right-click for JS/TS files
- [x] Runs `npx stryker run --mutate <relative-path>` in background task
- [x] Process runs from project root
- [x] Progress notification while running
- [x] Success notification with survived/killed summary
- [x] Console output in tool window
- [x] Error notification when npx not found
- [x] Stryker error output not swallowed
- [x] Error notification on non-zero exit code

### Slice 2: Run Stryker on a Folder (4 ACs)
- [x] Action appears for folders in Project View
- [x] Folder runs with `--mutate <folder-glob>`
- [x] Action hidden for non-JS/TS files
- [x] Only one run at a time per project

### Slice 3: Parse Report and Annotate Mutants (8 ACs)
- [x] Reads JSON report from `reports/mutation/mutation.json`
- [x] Survived mutants as warning annotations on exact lines
- [x] Tooltips show mutator name and replacement
- [x] Gutter icons on affected lines
- [x] Gutter icon hover lists all mutants on that line
- [x] Annotations cleared on new run
- [x] Notification when report file missing
- [x] Error notification for invalid JSON

## Key Decisions

1. **No interfaces, no abstraction layers** — one class per concern, direct IntelliJ Platform API usage
2. **MutationResultService as the only shared state** — runner writes, annotator reads, DaemonCodeAnalyzer.restart() triggers re-annotation
3. **Survived mutants only** — filtered at parse time, simplifies everything downstream
4. **npx-only** — no global Stryker fallback
5. **Default report path only** — `reports/mutation/mutation.json`, no custom config (YAGNI)
6. **PathUtil extracted during review** — eliminated duplicated path logic between action and annotator

## Build & Test

```bash
./gradlew build    # Compile + test + verify
./gradlew test     # Run all 83 tests
```
