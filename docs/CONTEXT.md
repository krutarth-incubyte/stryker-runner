# Stryker Runner — Session Context

## What This Is
A JetBrains IDE plugin (Kotlin, IntelliJ Platform SDK) for running StrykerJS mutation testing.
- Right-click JS/TS file or folder → "Run Stryker" → inline annotations for survived mutants
- GitHub repo: https://github.com/krutarth-incubyte/stryker-runner
- Local path: `/Users/kdave/Desktop/stryker-webstorm-plugin` (rename to `stryker-runner` manually)

## Tech Stack
- Kotlin 2.3.20, JVM 21, Gradle 9.4.1
- IntelliJ Platform Gradle Plugin 2.13.1
- JUnit 4 via `BasePlatformTestCase`
- `./gradlew build` | `./gradlew test` | `./gradlew buildPlugin`
- GitHub account: `krutarth-incubyte`

## Architecture
Feature-based packages under `com.github.nicetester.stryker`:

```
action/     RunStrykerAction, FetchCiReportAction, ClearStrykerResultsAction
runner/     StrykerRunner (Task.Backgroundable), StrykerConsole, StrykerToolWindowFactory
model/      MutantStatus (enum), MutantResult, MutantLocation, MutationReport, ReportParser
annotator/  MutantAnnotator (ExternalAnnotator), MutantGutterIconRenderer
service/    MutationResultService (project @Service — shared state)
util/       PathUtil, EnvironmentUtil, StrykerNotifications
ci/         GitUtil (branch/remote detection), GitHubArtifactClient (gh CLI)
```

**Dependency direction:** `action → runner → service ← annotator`, all through `model`. No cycles.

**Key rule:** `MutationResultService` is the single shared-state boundary. All sources funnel through `setResults()` or `setResultsIfNewer()`.

## Data Sources (3, but effectively 2)
| Source | When | Timestamp used |
|--------|------|----------------|
| **Local run** (Run Stryker button) | User clicks, Stryker executes | `mutation.json` file `lastModified` |
| **Auto-discovery** | File opened, report already on disk | `mutation.json` file `lastModified` |
| **CI fetch** | User clicks "Fetch from CI" | GitHub Actions workflow `updated_at` |

Sources 1+2 are the same file — local run writes it, auto-discovery reads it. CI is the only truly separate source.

**Priority rule: newest timestamp wins.** `setResultsIfNewer()` rejects any report older than what's currently loaded. This prevents auto-discovery from silently overwriting a fresher CI report when you open a file.

## Report Lifecycle
- **Local** (`reports/mutation/mutation.json`): written by Stryker, persists until next run overwrites it
- **CI download** (`.stryker-runner/ci-report/`): single directory, deleted before each new fetch, also deleted by "Clear Stryker Results"
- **In-memory** (`MutationResultService.resultsByFile`): cleared on "Clear", on new run start, on new fetch

## Key Design Decisions
- `MutantStatus` enum — never raw strings for status
- `ReportParser` preserves ALL statuses — filtering to survived happens at display time via `getSurvivedResultsForFile()`
- `tryMarkRunning()` uses `AtomicBoolean.compareAndSet` — race-free concurrent run guard
- `setResultsIfNewer(results, configDir, timestamp)` — all sources compete on timestamp; no implicit priority
- Single CI report dir (`.stryker-runner/ci-report/`) — not per-branch; old report deleted before new fetch
- `reportTimestamp = 0L` means nothing loaded; `ClearStrykerResultsAction` resets to 0
- `ClearStrykerResultsAction` only visible when `reportTimestamp > 0` (results are loaded)
- SSH org-alias URLs (`org-12345@github.com`) supported in `GitUtil.parseOwnerRepo()`
- `.stryker-runner/` and `.claude/*.local.md` are gitignored
- GitHub Actions workflows disabled (moved to `.github/workflows-disabled/`)

## Files of Note
- `plugin.xml` — 3 actions, tool window, notification group, external annotator
- `StrykerBundle.properties` — all i18n keys
- `CLAUDE.md` — architecture rules, conventions, build commands
- `docs/ROADMAP.md` — full feature backlog
- `docs/specs/stryker-mutation-testing-spec.md` — original spec (all ACs done)

## CI Fetch Flow
`FetchCiReportAction` → background task:
1. `GitUtil.getCurrentBranch()` → `git rev-parse --abbrev-ref HEAD`
2. `GitUtil.getRemoteOwnerRepo()` → `git remote get-url origin` → regex parse
3. `service.deleteCiReportDir()` → deletes `.stryker-runner/ci-report/`
4. `GitHubArtifactClient.fetchMutationReportForBranch()`:
   - `gh pr view <branch>` → PR number
   - `gh api .../actions/runs?event=pull_request&status=completed` → `RunInfo(id, timestamp)` from `updated_at`
   - `gh api .../actions/runs/<runId>/artifacts` → artifact name containing "mutation"/"stryker"
   - `gh run download <runId> --name <artifact> --dir .stryker-runner/ci-report/`
   - Walk dir → find `mutation.json`
5. `ReportParser.parse()` → `service.setResultsIfNewer(results, configDir, runTimestamp)`

**CI requirement:** Workflow must upload `mutation.json` as artifact with "mutation" or "stryker" in name.

## Test Count
| File | Tests |
|------|-------|
| RunStrykerActionTest | 33 |
| ReportParserTest | 17 |
| MutationResultServiceTest | 18 |
| PathUtilTest | 15 |
| MutantAnnotatorTest | 13 |
| StrykerRunnerTest | 8 |
| GitUtilTest | 9 |
| **Total** | **113** |

## Known Issues / TODO
- `findLatestWorkflowRun` has a dead `runGh` call at the top — leftover from earlier iteration, should clean up
- No settings UI — GitHub token/workflow name/report path all rely on defaults/gh CLI
- Plugin not yet published to marketplace
- Folder rename pending: local `stryker-webstorm-plugin` → user should `mv` to `stryker-runner`

## Roadmap Status
See `docs/ROADMAP.md`. Completed: CI fetch (v1), Clear Action, Auto-discovery, Timestamp priority.
Next priorities: file watcher for auto-refresh, configurable report path, mutation score panel.
