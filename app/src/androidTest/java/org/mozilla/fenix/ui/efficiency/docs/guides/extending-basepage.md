# Extending BasePage (primitives) — the exception path

Reach for this only when the interaction/assertion gate fails: a test needs something the existing
verbs can't express. Extending is rare on purpose — most tests compose what's already here. When you
do extend, add the smallest _general_ capability, because every test inherits it.

## First, check what already exists (don't reinvent)

Interaction verbs on `BasePage`: `mozClick`, `mozLongClick`, `mozClickIfPresent`,
`mozClickFirstWithParentText`, `mozEnterText`, `mozClear`, `mozClearAndEnterText`, `mozPressEnter`,
`mozSwipeTo` (scroll until visible), `mozSwipeElement` (directional), `mozPressBackUntilGone`,
`mozOpenNotificationsTray`.

Verification verbs: `mozVerify`, `mozVerifyElementAbsent`, `mozWaitUntilAbsent`,
`mozVerifyElementsByGroup`, `mozVerifyAnyContainsText`, `mozVerifyAnyHasChildWithText`,
`mozVerifyNoneContainText`, and state checks (`IsSelected` / `NotSelected` / `Enabled` /
`NotEnabled` / `Checked` / `NotChecked`, sibling/child-text variants). Navigation: `navigateToPage`.

If one of these fits, compose — stop here.

## Decide: primitive vs page-object helper

- **Add a `moz*` primitive** when the capability is generic across screens (a new interaction gesture
  or a new kind of assertion). It belongs on `BasePage` and must dispatch across the element backends
  (`Selector` can resolve via Espresso, UiAutomator, or Compose), matching how existing verbs handle
  all backends — don't hard-code one.
- **Add a typed helper on the page object** when the logic is specific to one screen (e.g.
  `BookmarksPage.createFolder(name)` wrapping a few steps). This keeps `BasePage` general and the
  screen-specific flow discoverable.

## How to add a primitive

Model it on an existing verb of the same shape. Keep the signature `moz…(selector: Selector, …):
BasePage` and `return this` so it chains. Handle the not-found / timeout path consistently with
siblings (the harness retries at the `BaseTest` level). Add a one-line doc explaining _why_ it
exists and when to use it.

## Harness gaps worth naming (not yet abstracted)

These are currently hand-wired in tests rather than harness verbs — if a conversion needs one, that's
a candidate for a real primitive/helper, and worth flagging:

- Permissions (`GrantPermissionRule`), account sign-in, and app-restart flows.
- A launcher-intent launch (some flows only appear via a launcher intent; the efficiency
  `HomeActivityIntentTestRule` path may need this option — verify against onboarding).
- Scroll as a first-class step (today it's a `requiresScroll` selector-group side effect).

## Gotchas

- Resist test-specific primitives — if only one test would ever call it, it's probably a page-object
  helper or just inline steps, not a `BasePage` verb.
- A new primitive that only handles one backend will silently fail on screens using another; cover
  all three or scope it clearly.
