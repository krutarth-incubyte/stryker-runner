# Spec: Stryker Mutation Testing Plugin

## Overview
A JetBrains IDE plugin that lets developers run StrykerJS mutation testing on files or folders directly from the Project View context menu, then displays survived mutants as inline annotations and gutter icons in the editor.

## Slice 1: Run Stryker on a Single File

The walking skeleton -- right-click a file, run Stryker, see output in a tool window.

- [x] A "Run Stryker" action appears in the Project View right-click menu when a JS/TS file is selected
- [x] Clicking the action runs `npx stryker run --mutate <relative-file-path>` in a background task with a progress indicator
- [x] The process runs from the project root directory
- [x] While Stryker is running, the user sees a progress notification (e.g., "Running Stryker...")
- [x] When the run completes successfully, the user sees a notification with a summary (e.g., "Stryker finished -- 3 survived, 7 killed")
- [x] Stryker console output is visible in a tool window or run console
- [x] When `npx` is not found on the system PATH, the user sees an error notification explaining that Node.js/npx is required
- [x] When no `stryker.conf` (or equivalent config) exists in the project, Stryker's own error output is shown to the user (not swallowed)
- [x] When Stryker exits with a non-zero code, the user sees an error notification with the exit code

## Slice 2: Run Stryker on a Folder

Extends Slice 1 to support folders and multi-selection.

- [x] The "Run Stryker" action also appears when a folder is selected in the Project View
- [x] Selecting a folder runs Stryker with `--mutate <folder-glob>` so all JS/TS files within it are mutated
- [x] The action is hidden (not shown) for non-JS/TS files (e.g., images, JSON, markdown)
- [x] Only one Stryker run can execute at a time per project; starting a second run shows a warning that a run is already in progress

## Slice 3: Parse Report and Annotate Survived Mutants

After a successful run, display survived mutants inline in the editor.

- [x] After a successful Stryker run, the plugin reads the JSON mutation report from the default report path (`reports/mutation/mutation.json` relative to project root)
- [x] Survived mutants are shown as warning annotations on the exact line in the source file
- [x] Each annotation tooltip shows the mutator name and replacement (e.g., "ConditionalExpression: replaced `&&` with `||`")
- [x] A gutter icon appears on each line that has one or more survived mutants
- [x] Hovering over the gutter icon shows a tooltip listing all survived mutants on that line
- [x] Annotations and gutter icons are cleared when a new Stryker run starts
- [x] When the report file does not exist after a run, the user sees a notification suggesting they check their Stryker config for JSON reporter
- [x] When the report file contains invalid JSON, the user sees an error notification instead of a crash

## Out of Scope
- Global Stryker installation fallback (npx only)
- Stryker configuration editing or generation from within the IDE
- Support for non-JS/TS mutation frameworks (e.g., Stryker4s, Stryker.NET)
- Killed/timed-out mutant annotations (only survived mutants are shown)
- Custom report path configuration (only default path)
- Test-level drill-down (which test killed which mutant)
- Mutation score display in a dashboard or summary panel

## Technical Context
- **Package:** `com.github.nicetester.stryker`
- **Plugin descriptor:** `src/main/resources/META-INF/plugin.xml`
- **i18n:** `StrykerBundle` with `messages/StrykerBundle.properties`
- **Patterns to follow:** IntelliJ Platform conventions -- `AnAction` for context menus, `Task.Backgroundable` for async work, `ExternalAnnotator` for editor annotations, `GutterIconRenderer` for gutter icons
- **Test framework:** JUnit 4 via `BasePlatformTestCase`, run with `./gradlew test`
- **Build:** Kotlin 2.3.20, Gradle 9.4.1, IntelliJ Platform Gradle Plugin 2.13.1, JVM 21
- **Stryker report schema:** JSON with `files` map, each containing `mutants` array with `id`, `mutatorName`, `replacement`, `location` (line/column), and `status` fields. Positions are 1-based.
- **Risk level:** MODERATE
