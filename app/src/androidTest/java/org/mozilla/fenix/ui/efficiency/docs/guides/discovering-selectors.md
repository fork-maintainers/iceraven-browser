# Discovering an element's real handles (before you author a selector)

Choose a selector strategy from **ground truth**, not from a stub or from how a legacy robot matched.
This is the step before `authoring-selectors.md` (which is about writing the catalog entries).

## First: which situation are you in?

- **You just have the debug app running (no test)** — the common case while exploring. You do NOT kick off
  instrumentation or attach a debugger to see elements. Two options that work against a plain (debuggable)
  build, below. This covers the **native/app** layer, which is most selectors.
- **You want the exact trees the harness sees** (Compose + Espresso + UIAutomator, with types) — that runs
  _inside_ an instrumented test via `ScreenDump`/`effdump`; see "Dump the screen — all three layers" below.

### Option A — raw `adb` uiautomator dump (CLI, no test, fastest)

Dumps the current screen's native/UIAutomator elements. Every node's `resource-id`, `text` and
`content-desc` is in the XML, which is what decides the strategy (app res-id →
`ESPRESSO_BY_ID`/`UIAUTOMATOR_WITH_RES_ID`; content-desc → content-description; text → last resort; a
prefix-less id → the raw-resourceId/web strategy):

```
adb shell uiautomator dump /sdcard/window_dump.xml
adb exec-out cat /sdcard/window_dump.xml
```

Fast and dependency-free, but it is raw XML with no ranking or screenshot. For anything more than a quick
look, use Option C — `effview` does this parsing for you and overlays the result on a screenshot.

### Option B — Android Studio **Layout Inspector** (GUI, no test)

`View ▸ Tool Windows ▸ Layout Inspector`, attach to the running app process. It shows the **View tree AND
Compose composables**, with attributes/semantics. Best for Compose-heavy screens, and the only option that
shows a `testTag` that is _not_ published to the accessibility tree (see the boundary section below).

### Option C — the in-test inspector (all three layers, incl Compose testTags) ⭐ recommended

The best of both: real Compose testTags + Espresso + UIAutomator, **and** an interactive click-around loop —
no breakpoint, no Evaluate Expression. Run the inspector (it keeps the app alive ~30 min without pausing),
then one command per screen:

```
# 1) start the interactive inspector, leave it running:
./mach gradle fenix:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=org.mozilla.fenix.ui.efficiency.devtools.InteractiveInspectTest#inspect
# 2) navigate the app by hand to the screen you care about
# 3) dump + view it (repeat 2–3 per screen):
devtools/effview/effview.sh
```

`effview.sh` drops a trigger file → `ScreenDump.dumpAll()` writes a screenshot + a 3-layer element JSON to the
app's internal storage → the script pulls both with `adb exec-out run-as` → renders `screen.html`: the
screenshot with every element boxed, **color-coded by layer** (Compose / Espresso / UIAutomator) and mapped to
its suggested selector. For a single dump without the loop: run `InteractiveInspectTest#dumpOnce`, then
`devtools/effview/effview.sh --no-trigger`. Retrieval uses `run-as` (not `adb pull`) because the artifacts live in the
app's internal `filesDir` — that's what makes them reachable on a scoped-storage device.

### The honest boundary (why "just dump everything" isn't possible)

- **Compose `testTag`s** are a Compose-_test_ concept, and by default they are NOT in the
  UIAutomator/accessibility tree. The exception matters, because Fenix relies on it: a composable that also
  sets `testTagsAsResourceId = true` publishes its tag as the node's **resource-id** (un-prefixed), so it
  DOES show up in an `adb` dump and can be matched device-side — that is exactly what
  `UIAUTOMATOR_WITH_COMPOSE_TAG` does, and why we can avoid Compose sync on screens where GeckoView is
  active. Tags without that flag are invisible to `adb`; see them via Layout Inspector (Option B) or an
  in-test `ScreenDump` (below).
- **Web / GeckoView DOM elements** (a page's `submit`/`username` inputs) render inside one native view, so
  they're **not** in any native dump. Web ids are matched with the raw-resourceId strategy, discovered by
  reading the page or an in-test dump — not from uiautomator.

## The three layers (what Option C captures)

`ScreenDump.dumpAll()` (Option C, via `helpers/ScreenDump.kt`) captures what handles an element exposes across
**three** layers, because no single one is complete:

- **Compose** — `testTag`, text, content-description for Compose UI.
- **Espresso** (in-process view tree) — id / class / text / hint / content-desc for View-based screens,
  including id-less views the accessibility tree omits.
- **UIAutomator** — res-id / text / content-desc across process boundaries (system dialogs, GeckoView).

Rule of thumb: Compose screens → Compose dump; legacy **View** screens (RecyclerViews, res-id widgets)
dump **0 Compose nodes** — author those from the Espresso dump first, then UIAutomator for
cross-process/system/web. (Lesson C4.)

To capture on a navigation failure the harness already fires a `ScreenDump`; read it with
`adb logcat -c` then `adb logcat -d -s EffScreenDump:I` (gotcha A6).

## Selector priority (pick the most stable handle available)

`testTag` → resource id → content-description → **text (last resort, always via `getStringResource(...)`)**.
Text collides and breaks on localization. If a `testTag` exists, use it even when text "looks" fine
(lesson C1 / gotcha A7 — the trust-panel title also renders in the address bar, so text matched multiple
nodes).

**But do not silently drop the content check when you switch to a tag.** If the text was the thing being
asserted — "the panel is describing _this_ site" — matching the tag alone reduces the assertion to "an
element with this tag exists", which is nearly always true and passes for the wrong reason. Match both
with `COMPOSE_BY_TAG_AND_TEXT` (tag in `value`, expected text in `secondaryValue`): the tag disambiguates,
the text still asserts the content.

## Three traps that will bite you

1. **Never trust a stubbed page object's locators (C2).** Pre-stubbed handles are frequently STALE — e.g. a
   custom-tab menu button stubbed as the old Android-view res-id (`mozac_browser_toolbar_menu`) when the
   redesigned toolbar uses a content-description ("More options"). Dump and verify every stub before relying on it.
2. **Web DOM ids need the RAW-resourceId strategy (C5).** `UIAUTOMATOR_WITH_RES_ID` prepends
   `packageName:id/` — correct for APP views, wrong for WEB/GeckoView ids (`submit`, `username`, `password`).
   Those surface as raw resource-ids with no prefix; match them with the raw-resid strategy
   (`UIAUTOMATOR_WITH_COMPOSE_TAG` matches the raw resourceId). (Do not assume the raw-resid strategy is
   dead in the MTE-5722 consolidation — web ids need it.)
3. **Arrival / entry selectors must cover EVERY runtime state (B7 / C3).** `requiredForPage` must be an
   element present in ALL states — a toolbar title, never an empty-list placeholder (RecentlyClosed's
   arrival signal vanished once the list was populated). If an entry control's handle changes with state
   (the trust-panel site-info button is `SITE_INFO_SECURE` / `_UNSECURE` / `_UNKNOWN` by page security),
   the nav edge must `ClickIfPresent` every variant. `effcheck` can't see this — verify by hand when you
   build or modify navigation.

Once you know the real handle and strategy, go to `authoring-selectors.md` to write the catalog entry
(selectors live in `selectors/*Selectors.kt`, never inline in a page object — gotcha B2).
