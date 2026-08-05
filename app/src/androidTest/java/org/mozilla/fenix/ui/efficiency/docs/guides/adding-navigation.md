# Adding navigation nodes and edges

The navigation graph is what lets a test say `on.settings.navigateToPage()` and have the harness
route there from wherever it is. Nodes are `pageName` strings; edges are registered steps between
them. `navigateToPage()` does a BFS over the graph, replays the steps, then confirms arrival via the
target's `requiredForPage` selectors.

## Registering an edge

In the page object's `init { }`:

```kotlin
NavigationRegistry.register(
    from = "HomePage",          // a pageName, or "AppEntry" for launch
    to = pageName,              // usually this page
    steps = listOf(
        NavigationStep.Click(HomeSelectors.MAIN_MENU_BUTTON),
        NavigationStep.Click(MainMenuSelectors.SETTINGS_BUTTON),
    ),
)
```

Register the edge on the page you're modeling (the `to`), describing how you get in from its
parent(s). A page can be reached from several parents — register multiple edges.

## NavigationStep types

`Click`, `ClickIfPresent`, `Swipe`, `EnterText`, `PressEnter`, `PressBack`, `PressBackUntilGone`,
`Action`, `OpenNotificationsTray`, `WaitForIdle`. Compose steps from these; each takes the selector
(or value) it operates on. If a step needs an interaction the set doesn't cover, that's a harness
gap — see `extending-basepage.md`.

## Special nodes and cases

- **`AppEntry`** is the synthetic root the app launches into. Most screens are reached transitively
  from `HomePage` (which registers `AppEntry -> HomePage`). Only register `from = "AppEntry"`
  directly for launch-time surfaces.
- **Launch-flag entries.** Some flows only exist under a launch configuration. Onboarding only shows
  when the app launches with it enabled, so `OnboardingPage` registers `AppEntry -> OnboardingPage`
  with empty steps, and its tests declare `BaseTest(skipOnboarding = false)`. The nav edge and the
  launch flag go together — document the required flag on the page object.
- **Empty steps** mean "already here at entry" — only valid for `AppEntry -> X` launch surfaces.

## Verify the path resolves

After adding an edge, confirm the graph can actually route it before writing the test:

- Run with `-PlogNavigationSummary` (BaseTest logs the path summary) or use the devtools reachability
  generator/logger to see the computed path.
- If BFS can't find a path, a middle edge is missing — add the intermediate page's edges too.

## Gotchas

- Edges are directed. `A -> B` does not give you `B -> A`; register both if both are used.
- A wrong/rearranged step sequence is the most common cause of a nav that "can't find the page" —
  mirror the exact click order a user performs.
- Keep `requiredForPage` honest on the target, or a _successful_ route will still fail verification.
