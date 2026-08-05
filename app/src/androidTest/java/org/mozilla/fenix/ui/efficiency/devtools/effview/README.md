# effview — Interactive Selector Discovery Tool

Visual selector discovery for test authoring. Click any element on a screenshot to see ALL matching selectors across Compose, Espresso, and UIAutomator layers, ranked by preference.

![effview example](example_effview.png)

_Click any element on screen to see all matching selectors, ranked by quality, with ready-to-paste code snippets._

## What It Does

1. **Captures** the current screen: screenshot + all UI elements from 3 layers (Compose, Espresso, UIAutomator)
2. **Generates** an interactive HTML overlay where you can click elements to see selectors
3. **Ranks** selectors by quality: Compose testTag > resource-id > content-description > text
4. **Detects** overlapping elements and shows all matching options
5. **Generates** ready-to-paste Kotlin selector code

## Quick Start

**1. Start the interactive inspect test** (keeps app alive 30 min):

```bash
cd mobile/android/fenix
./mach gradle fenix:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.mozilla.fenix.ui.efficiency.devtools.InteractiveInspectTest#inspect
```

**2. Navigate to any screen** in the app (manually drive it)

**3. Trigger a dump and view it**:

```bash
cd app/src/androidTest/java/org/mozilla/fenix/ui/efficiency/devtools/effview
./effview.sh
```

The browser opens showing the screen with clickable element overlays!

**4. Repeat steps 2-3** for each screen you want to explore (no rebuild needed)

## What You'll See

- **Screenshot** with colored boxes over every interactive element
- **Click any box** → see all matching selectors ranked by quality
- **⭐ Recommended** selector (best layer + strategy)
- **Code snippets** with copy buttons for immediate use
- **All Selectors list** (searchable, filterable by clickable elements)
- **Page detection** (e.g., "HomePage" → paste selectors in `HomePageSelectors.kt`)

## Example Output

```kotlin
// Compose layer (recommended)
val TOP_SITES = Selector(
    strategy = SelectorStrategy.COMPOSE_BY_TAG,
    value = "top_sites.component",
    description = "Top sites section",
    groups = listOf("homepage")
)
```

## Files

- `effview.sh` - Main script (trigger → pull → render → open)
- `effviewdump-v2.py` - HTML generator with overlap detection and smart ranking
- `README.md` - This file

## Tips

- **Multiple matching selectors?** The tool shows them all! Pick the most stable one for your test.
- **Element not appearing?** Check if a system overlay/keyboard is covering it (the tool captures all layers).
- **Wrong page detected?** It infers from test tag prefixes. Override by checking the "All Selectors" list.
- **No screenshot?** The test might have exited. Restart `InteractiveInspectTest#inspect`.

## Advanced Usage

**Pull without triggering a new dump** (if you already have one):

```bash
./effview.sh --no-trigger
```

**Use a different package**:

```bash
PKG=org.mozilla.firefox.debug ./effview.sh
```

**Custom output directory**:

```bash
OUT=./my-dumps ./effview.sh
```

## Troubleshooting

**"run-as: unknown package"**

- The app isn't running or already stopped
- Solution: Make sure `InteractiveInspectTest#inspect` is still running (it stays alive 30 min)

**"No such file: eff-screendump.json"**

- No dump has been triggered yet
- Solution: Touch the trigger file: `adb shell run-as org.mozilla.fenix.debug touch files/eff-scan`

**Viewer shows empty/wrong elements**

- The dump might be stale
- Solution: Navigate to your target screen, then run `./effview.sh` again

## How It Works

1. `effview.sh` drops a trigger file (`files/eff-scan`) that `InteractiveInspectTest#inspect` watches
2. The test calls `ScreenDump.dumpAll()` which captures:
   - Compose semantics (test tags, text, content descriptions)
   - Espresso view hierarchy (resource IDs, text)
   - UIAutomator accessibility tree (native layer)
   - Screenshot (PNG)
3. Everything is written to JSON + PNG in the app's internal storage
4. `effview.sh` pulls both files via `adb shell run-as`
5. `effviewdump-v2.py` generates an interactive HTML overlay
6. Browser opens automatically

## Future Enhancements

- Multi-screen navigation graph integration
- Selector usage statistics from existing tests
- Diff mode (compare two screens)
- Export to `*Selectors.kt` file format
