# Stryker Runner — Session Context

## What This Is
A JetBrains IDE plugin (Kotlin, IntelliJ Platform SDK) for running StrykerJS mutation testing.
- Right-click JS/TS file or folder → "Run Stryker" → inline annotations for survived mutants
- GitHub repo: https://github.com/krutarth-incubyte/stryker-runner
- Local path: `/Users/kdave/Desktop/stryker-webstorm-plugin`

## Tech Stack
- Kotlin 2.3.20, JVM 21, Gradle 9.4.1
- IntelliJ Platform Gradle Plugin 2.13.1
- JUnit 4 via `BasePlatformTestCase`
- `./gradlew build` | `./gradlew test` | `./gradlew runWebStorm`
- GitHub account: `krutarth-incubyte`

## Architecture
Feature-based packages under `com.github.nicetester.stryker`:

```
action/     RunStrykerAction (right-click run), FetchCiReportAction (CI download)
runner/     StrykerRunner (Task.Backgroundable), StrykerConsole, StrykerToolWindowFactory
model/      MutantStatus (enum), MutantResult, MutantLocation, MutationReport, ReportParser
annotator/  MutantAnnotator (ExternalAnnotator), MutantGutterIconRenderer
service/    MutationResultService (project @Service — shared state)
util/       PathUtil, EnvironmentUtil, StrykerNotifications
ci/         GitUtil (branch/remote detection), GitHubArtifactClient (gh CLI)
```

**Dependency direction:** `action → runner → service ← annotator`, all through `model`. No cycles.

**Key rule:** `MutationResultService` is the single shared-state boundary. Runner writes results, annotator reads them. All mutation status uses `MutantStatus` enum — never raw strings.

## Features Implemented
1. **Run Stryker** — right-click file/folder → `npx stryker run --mutate <pattern>` in background
2. **Monorepo support** — walks up from file to find nearest `stryker.conf.js` etc., runs from there
3. **Inline annotations + gutter icons** — survived mutants highlighted after run
4. **Auto-discovery** — opens file → auto-loads existing `reports/mutation/mutation.json` from disk (no manual step, like ESLint)
5. **Fetch from CI** — right-click → "Fetch Stryker Results from CI" → uses `gh` CLI to find PR, download artifact, load annotations
6. **Custom icons** — `/icons/runStryker.svg` and `/icons/fetchCiReport.svg` (with `_dark` variants)
7. **110 tests passing**

## Key Design Decisions
- `ReportParser` preserves ALL mutant statuses (Survived/Killed/Timeout/NoCoverage etc.) — filtering happens at display time via `getSurvivedResultsForFile()`
- `tryMarkRunning()` uses `AtomicBoolean.compareAndSet` — prevents concurrent run race condition
- CI artifact downloaded to `.stryker-runner/ci-reports/<branch>/` (gitignored)
- `gh` CLI used for GitHub API — no token config needed (reuses existing `gh auth`)
- `.claude/*.local.md` gitignored (bee session files)
- GitHub Actions workflows disabled (moved to `.github/workflows-disabled/`)

## Files of Note
- `plugin.xml` — registers actions, tool window, notification group, external annotator
- `StrykerBundle.properties` — all 30 i18n keys
- `CLAUDE.md` — architecture rules, conventions, build commands
- `docs/ROADMAP.md` — full feature backlog
- `docs/specs/stryker-mutation-testing-spec.md` — original spec (all ACs done)
- `docs/BUILD_RECAP.md` — initial build summary

## CI Fetch — How It Works
`FetchCiReportAction` → background task → `GitUtil.getCurrentBranch()` + `getRemoteOwnerRepo()` → `GitHubArtifactClient.fetchMutationReportForBranch()`:
1. `gh pr view <branch>` → PR number
2. `gh api repos/<owner>/<repo>/actions/runs?event=pull_request` → latest run ID matching PR
3. `gh api repos/<owner>/<repo>/actions/runs/<runId>/artifacts` → find artifact with "mutation"/"stryker" in name
4. `gh run download <runId> --name <artifact> --dir .stryker-runner/ci-reports/<branch>/` → extract
5. Walk downloaded dir for `mutation.json` → `ReportParser.parse()` → `MutationResultService.setResults()`

**User requirement for CI:** Workflow must upload `mutation.json` as GitHub Actions artifact with "mutation" or "stryker" in artifact name.

## Roadmap (from docs/ROADMAP.md)
**High priority:** Mutation Score Summary Panel, Clear Annotations Action, File Watcher for Auto-Refresh, Configurable Report Path
**Medium:** Status-based colors (NoCoverage=red, Survived=yellow), Run on Git Changes only, Progress parsing, Keyboard shortcut
**Low:** Inline diff preview, Stryker config generator, Mutation score trend, Multi-framework support, Incremental mutation testing, CI auto-polling (v2 of current fetch)

## Known Issues / TODO
- `FetchCiReportAction`: The `findLatestWorkflowRun` logic has a dead code path (unused `runGh` call at top of function before the real impl). Should clean up.
- No settings UI yet — GitHub token, workflow name, report path all rely on defaults/gh CLI
- Plugin ID is `com.github.nicetester.stryker` — not published to marketplace yet
- Folder rename pending: local folder is `stryker-webstorm-plugin`, repo is `stryker-runner` — user needs to `mv stryker-webstorm-plugin stryker-runner` manually

## Test Count by File
| File | Tests |
|------|-------|
| RunStrykerActionTest | 33 |
| ReportParserTest | 17 |
| MutationResultServiceTest | 18 |
| PathUtilTest | 15 |
| MutantAnnotatorTest | 13 |
| StrykerRunnerTest | 8 |
| GitUtilTest | 7 |
| **Total** | **110** |
