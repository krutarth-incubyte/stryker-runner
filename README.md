# Stryker Runner

A JetBrains IDE plugin that brings [StrykerJS](https://stryker-mutator.io/) mutation testing directly into your editor. Run mutation tests from the Project View, and see survived mutants highlighted inline with warning annotations and gutter icons.

Works in **WebStorm, IntelliJ IDEA, PhpStorm**, and all JetBrains IDEs with JavaScript/TypeScript support.

<!-- Plugin description -->
Run StrykerJS mutation testing on files or folders directly from the right-click context menu. Survived mutants are highlighted inline in the editor with warning annotations and gutter icons, showing exactly which mutations your tests didn't catch.
<!-- Plugin description end -->

## Features

- **Right-click to run** — Select a JS/TS file or folder in Project View, click "Run Stryker"
- **Inline annotations** — Survived mutants appear as yellow warning highlights on the exact line and column
- **Gutter icons** — Warning icons in the gutter for quick visual scanning
- **Tooltips** — Hover to see the mutator name and replacement (e.g., "Survived mutant: ConditionalExpression — replaced with `false`")
- **Auto-discovery** — Automatically loads existing `mutation.json` reports when you open a file, no manual step needed. Works with reports from terminal runs, CI, or previous plugin runs
- **Monorepo support** — Finds the nearest `stryker.conf.js` and runs Stryker from the correct directory
- **Live console output** — "Stryker Output" tool window shows real-time Stryker output
- **Concurrent run prevention** — Only one Stryker run at a time per project

## Requirements

- **Node.js** with `npx` on your system PATH
- **StrykerJS** configured in your project (a `stryker.conf.js`, `stryker.conf.mjs`, `stryker.config.json`, or similar)
- The **JSON reporter** enabled in your Stryker config (so the plugin can read `reports/mutation/mutation.json`)

### Enabling the JSON Reporter

Add the JSON reporter to your Stryker config:

```js
// stryker.conf.js
module.exports = {
  reporters: ['html', 'clear-text', 'progress', 'json'],
  // ... your other config
};
```

## Installation

### From Disk (for now)

1. Clone this repo and build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
2. Find the ZIP at `build/distributions/Stryker Runner-<version>.zip`
3. In your JetBrains IDE: **Settings > Plugins > Gear icon > Install Plugin from Disk** > select the ZIP

### From JetBrains Marketplace (coming soon)

The plugin will be published to the JetBrains Marketplace for one-click installation.

## Usage

### Run Mutation Testing

1. Right-click a `.ts`, `.js`, `.jsx`, `.tsx`, `.mjs`, or `.mts` file (or folder) in the **Project View**
2. Click **"Run Stryker"**
3. Watch the progress in the **"Stryker Output"** tool window at the bottom
4. After completion, survived mutants appear as **yellow warning highlights** in the editor

### Auto-Discovery (No Run Needed)

If a `reports/mutation/mutation.json` already exists (from a terminal run, CI pipeline, etc.), simply **open a file** — the plugin will automatically find and display the mutation results. No manual step required.

## Supported File Types

`.js`, `.ts`, `.jsx`, `.tsx`, `.mjs`, `.mts`

## Building from Source

```bash
git clone https://github.com/krutarth-incubyte/stryker-runner.git
cd stryker-runner
./gradlew build        # compile + test
./gradlew runWebStorm  # launch sandboxed WebStorm with plugin
./gradlew buildPlugin  # build distributable ZIP
```

## License

See [LICENSE](LICENSE) for details.
