# Stryker Runner

JetBrains IDE plugin for running StrykerJS mutation testing from the Project View context menu.

## Build & Test

```bash
./gradlew build       # compile + test + verify
./gradlew test        # run all tests
./gradlew clean build # clean build
./gradlew runWebStorm # launch sandboxed WebStorm with plugin installed
./gradlew buildPlugin # build distributable ZIP
```

## Stack

- **Language:** Kotlin 2.3.20, JVM 21
- **Build:** Gradle 9.4.1 (Kotlin DSL), IntelliJ Platform Gradle Plugin 2.13.1
- **Test:** JUnit 4 via `BasePlatformTestCase`, run with `./gradlew test`
- **Platform:** IntelliJ Platform SDK (targets all JetBrains IDEs)

## Architecture

Feature-based packages within `com.github.nicetester.stryker`:

```
action/     → Context menu entry point (RunStrykerAction)
runner/     → Process execution + console output (StrykerRunner, StrykerConsole)
model/      → Data classes + JSON parsing (MutantStatus, MutantResult, ReportParser)
annotator/  → Editor annotations + gutter icons (MutantAnnotator, MutantGutterIconRenderer)
service/    → Shared state (MutationResultService)
util/       → Path helpers, environment checks, notifications (PathUtil, EnvironmentUtil, StrykerNotifications)
```

### Dependency Direction

```
action/ → runner/ → service/ ← annotator/
                  → model/
```

No cycles. `MutationResultService` is the single shared-state boundary — the runner writes results, the annotator reads them.

### Key Rules

- **No interfaces** — one implementation per concern. Extract interfaces only when a second implementation arrives.
- **All user-facing strings** go through `StrykerBundle.properties` via `StrykerBundle.message()`.
- **Notifications** use the shared `StrykerNotifications` utility, not inline `NotificationGroupManager` calls.
- **Environment checks** (`isNpxAvailable`, `npxCommand`) live in `EnvironmentUtil`, not in runner/action classes.
- **Mutant status** uses the `MutantStatus` enum, never raw strings.
- **Report parsing** preserves ALL mutant statuses; filtering to survived happens at display time in the annotator/service.
- **Concurrent run prevention** uses `AtomicBoolean.compareAndSet` via `MutationResultService.tryMarkRunning()`.

### Plugin Registration

- `plugin.xml` registers: action (ProjectViewPopupMenu), tool window, notification group, external annotator.
- Services use `@Service(Service.Level.PROJECT)` with `service<>()` for DI.

## Conventions

- **Naming:** Kotlin standard (PascalCase classes, camelCase functions/properties)
- **Test naming:** `testXxx` methods (JUnit 3/4 convention required by `BasePlatformTestCase`)
- **Test isolation:** Each test class uses `setUp`/`tearDown` to reset shared state
- **i18n keys:** dot-separated hierarchy (e.g., `notification.stryker.error.title`)
