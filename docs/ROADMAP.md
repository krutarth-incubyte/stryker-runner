# Stryker Runner — Feature Roadmap

## High Value — Next Up

### 1. Mutation Score Summary Panel
A tool window showing mutation score per file/folder — killed, survived, timeout, no coverage counts. Like a test coverage panel but for mutation testing. We already parse all statuses; just need the UI.

### 2. Clear Annotations Action
Right-click "Clear Stryker Results" to dismiss annotations when you're done reviewing. Currently they persist until a new run.

### 3. File Watcher for Auto-Refresh
Watch `mutation.json` for changes — when Stryker finishes (from terminal or CI), annotations update automatically without reopening the file.

### 4. Configurable Report Path
Settings page to customize the report path (instead of hardcoded `reports/mutation/mutation.json`). Some teams configure Stryker to output elsewhere.

## Medium Value — Quality of Life

### 5. Different Icons/Colors by Status
- Red for `NoCoverage` (most critical — no test covers this line at all)
- Yellow for `Survived` (current behavior)
- Gray for `Timeout`

Easy since we already have the `MutantStatus` enum and preserve all statuses.

### 6. Run on Changed Files Only
"Run Stryker on Git Changes" — detect modified files via `git diff` and run Stryker only on those. Huge time saver in daily workflow.

### 7. Quick-Fix: Jump to Test
Click on a survived mutant annotation → action to navigate to the test file that should have caught it. Uses Stryker's test mapping data if available.

### 8. Progress Parsing
Parse Stryker's stdout in real-time to show "Mutant 23/96 — 45% tested" in the progress bar instead of just "Running Stryker...".

### 9. Keyboard Shortcut
Assign a default keybinding (e.g., `Ctrl+Shift+M`) to run Stryker on the current file without right-clicking.

## Lower Priority — Differentiators

### 10. Inline Diff Preview
Hover a survived mutant → show a mini diff of what the mutant changed. "Line 42: `&&` was replaced with `||`" shown as an actual code diff, not just text.

### 11. Stryker Config Generator
"Initialize Stryker" action — generates a `stryker.conf.js` with sensible defaults for the detected project type (Jest, Vitest, Mocha).

### 12. Mutation Score Trend
Track mutation scores across runs (store in `.stryker-runner/history.json`). Show whether your mutation score is improving or regressing over time.

### 13. Multi-Framework Support
Support PIT (Java/Kotlin), Stryker.NET (C#), mutmut (Python). The architecture already has the extension points — needs a `MutationFramework` interface.

### 14. CI Integration ✅ v1 Implemented
Download and display mutation reports from GitHub Actions PR artifacts without needing a local Stryker run. Manual "Fetch from CI" button.

**Future:** Automatic background polling — check every 5 minutes or on branch switch.

### 15. Incremental Mutation Testing
Pass `--incremental` flag to Stryker automatically, using the previous report to skip already-tested mutants. Dramatically reduces run time on re-runs.
