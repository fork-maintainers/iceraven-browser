# Creating a page object

A page object models one screen (or a distinct component) so tests can reach it and read its state
without knowing _how_. It extends `BasePage`, names itself, registers how it's reached, and exposes
its selectors by group. Keep behavior in the harness/selectors — a page object is thin.

## Steps

1. **Decide page vs component.** A full screen is a `...Page`; a reusable sub-surface (toolbar,
   share sheet, tab drawer) is a `...Component`. Both extend `BasePage`; the suffix is convention.
2. **Create `pageObjects/<Screen>Page.kt`.**
   - `override val pageName = "<Screen>Page"` — this string is the node id used by the nav graph and
     `PageStateTracker`; keep it stable and matching the class.
   - In `init { }`, register how the page is reached (see `adding-navigation.md`).
   - `override fun mozGetSelectorsByGroup(group) = <Screen>Selectors.all.filter { it.groups.contains(group) }`
     — this is how `navigateToPage()` finds the `requiredForPage` locators to confirm arrival.
3. **Register it in `helpers/PageContext.kt`.** Add the import and a `val <camelName> =
<Screen>Page(composeRule)` in lexicographic order. This matters: `PageCatalog` discovers pages by
   reflecting over `PageContext`, and the reachability factory + `on.<name>` access depend on it.
4. **Add selectors** in a sibling `<Screen>Selectors.kt` (see `authoring-selectors.md`). At minimum,
   tag the element that proves you're on the screen with the `requiredForPage` group.

## Worked example (from the Onboarding pilot)

A launch-time flow with no inbound nav steps:

```kotlin
class OnboardingPage(composeRule: AndroidComposeTestRule<HomeActivityIntentTestRule, *>) : BasePage(composeRule) {
    override val pageName = "OnboardingPage"

    init {
        // Reached at launch when BaseTest(skipOnboarding = false); no steps needed.
        NavigationRegistry.register(from = "AppEntry", to = pageName, steps = listOf())
    }

    override fun mozGetSelectorsByGroup(group: String): List<Selector> =
        OnboardingSelectors.all.filter { it.groups.contains(group) }
}
```

A normal screen leaves the `init` registration to describe the click path from its parent — see
`adding-navigation.md` for that form.

## Gotchas

- `pageName` must be unique and stable; it's a graph key, not a label.
- If you forget the `PageContext` registration, the page is invisible to discovery and `on.<name>`
  won't compile — always do step 3.
- Don't put assertions or multi-step flows in the page object; those belong in the test or, if truly
  reusable, a typed helper method on the page object.
