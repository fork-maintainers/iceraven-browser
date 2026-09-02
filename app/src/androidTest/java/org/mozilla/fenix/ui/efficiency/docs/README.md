# ui/efficiency — Android UI test framework

A page-object + navigation-graph framework for Fenix UI tests. It makes UI tests **cheap to write and
cheap to maintain**: a test describes only the _what_ (reach this screen, do this, verify that); the
harness owns the _how_ (navigation, element resolution, retries). Most tests are ~5–20 lines.

## Why it exists (the two problems)

1. **Maintenance cost.** Hand-written UI tests duplicate selectors/navigation until upkeep outruns value
   and teams abandon them. Here the expensive layer (selectors, page objects, navigation) is centralized
   and shared, so a UI change is fixed once, not once per test.
2. **Trust in failures.** Product bugs, harness bugs, and environment noise get conflated. The harness
   attributes failures (structured `Eff` logs, on-failure screen dumps) so a red is actionable.

See `architecture.md` for the model and the rationale.

## Directory map

```
efficiency/
├── core/         resolve() seam, UiElement facade, ScreenDump — the low-level element plumbing
├── helpers/      BaseTest, BasePage (the moz* verb library + navigateToPage BFS routing)
├── navigation/   NavigationEdge · NavigationRegistry · NavigationStep · PageCatalog  (the 4-file graph core)
├── generation/   case-building infra + factories (reachability / pairs / interaction / behavior)
├── devtools/     dev/debug tools, atomic test runners, effpretty (log renderer)
├── logging/      structured test logging (the `Eff` logcat tag)
├── pageObjects/  one <Screen>Page.kt per screen — models it, registers how to reach it
├── selectors/    one <Screen>Selectors.kt per screen — the element locator catalog
├── tests/        the actual @Test classes
└── docs/         you are here
```

## Quickstart — a minimal test

```kotlin
class BookmarksTest : BaseTest() {
    private val mockWebServer get() = fenixTestRule.mockWebServer

    @SmokeTest @Test
    fun openBookmarkInNewTabTest() {
        val page = mockWebServer.getGenericAsset(1)
        on.browserPage.navigateToPage(page.url.toString())   // reach a state
        on.mainMenu.navigateToPage()                          // route via the nav graph
            .mozClick(MainMenuSelectors.BOOKMARKS_BUTTON)     // interact
        on.bookmarks.navigateToPage()                         // arriving here verifies it opened
    }
}
```

`on` is the `PageContext` (every modeled screen hangs off it). `navigateToPage()` BFS-routes over the
graph. Selectors are referenced from their catalog (`<Screen>Selectors.NAME`), never inlined.

## How you actually work

Follow the gate loop in **`converting-a-test.md`** (the daily driver for converting a legacy smoke test),
and read the building-block guide for whatever piece is missing:

| To…                                                       | Read                               |
| --------------------------------------------------------- | ---------------------------------- |
| Convert a legacy test end-to-end                          | `converting-a-test.md`             |
| Find an element's real handles before choosing a selector | `guides/discovering-selectors.md`  |
| Add locators to a catalog                                 | `guides/authoring-selectors.md`    |
| Model a new screen                                        | `guides/creating-a-page-object.md` |
| Reach a screen / add graph edges                          | `guides/adding-navigation.md`      |
| Compose the test method                                   | `guides/writing-a-test.md`         |
| Add a `moz*` verb or page helper                          | `guides/extending-basepage.md`     |
| Run & debug a test                                        | `guides/debugging-tests.md`        |
| The harness gotchas + review checklist                    | `gotchas.md`                       |
| The helper scripts (run by hand)                          | `tooling.md`                       |

## Best practices

- Tests describe the _what_; wrap all Espresso/UIAutomator/Compose in page objects and `moz*` verbs.
- Reuse an existing capability before adding one; add the smallest general block, not a test-specific hack.
- Selector priority: Compose `testTag` → resource id → content-description → text (last resort).
- Verify handles against the live UI (dump the screen), not against how a legacy robot matched.
- A test that only passes on retry is flaky, not done.

---

_Maintenance note:_ these docs are the human source of truth; the `efficiency-test-authoring` skill distills
them for agent use — keep them in sync.
