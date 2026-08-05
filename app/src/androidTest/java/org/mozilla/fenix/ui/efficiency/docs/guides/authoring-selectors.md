# Authoring selectors

A `Selector` is one way to find one element. They live in `selectors/<Screen>Selectors.kt` as an
`object` of vals plus an `all` list. The test/harness never hard-codes matchers — it references
these, so a UI change is fixed in one place.

## Anatomy

```kotlin
val CONTINUE_BUTTON = Selector(
    strategy = SelectorStrategy.COMPOSE_BY_TEXT,
    value = getStringResource(R.string.nova_onboarding_continue_button),
    description = "Onboarding Continue button",
    groups = listOf("continueButton"),
)
```

- **strategy** — how to match. Pick the one matching how the legacy robot found it.
- **value** — the matcher input. Prefer stable inputs: `getStringResource(R.string.x)` for localized
  text, test tags, or resource ids — not hard-coded English (breaks under localization).
- **description** — human-readable; shows up in logs.
- **groups** — tags that let the harness fetch sets of selectors (below).

End the file with `val all = listOf(...)` containing every selector — `mozGetSelectorsByGroup`
filters this.

## Choosing a strategy — priority order (important)

Derive the selector from the **app UI source** (`app/src/main/...`), NOT from how the legacy robot
matched. Legacy robots lean on visible text because it was convenient to write; the efficiency
framework prefers stable, language-independent identifiers so a copy tweak or a localized build
doesn't break the test. Open the composable/view for the element and use the FIRST handle that
exists, in this order:

1. **Compose test tag → `COMPOSE_BY_TAG`.** Look for `Modifier.testTag("x")` or
   `.semantics { testTag = "x" }` on (or wrapping) the element. Most stable Compose handle.
   `value = "<the tag>"` (e.g. `"homepage.view"`, `"top_sites_list"`).
2. **Resource id → `ESPRESSO_BY_ID` (View) or `UIAUTOMATOR_WITH_RES_ID`.** Look for `android:id=` in
   XML or a `resourceId`. `value = "<id name>"` (no `R.id.` prefix).
3. **Content description → `COMPOSE_BY_CONTENT_DESCRIPTION` / `ESPRESSO_BY_CONTENT_DESC`.** Use when
   the element sets a stable `contentDescription` (common on icon buttons). Prefer a string resource
   over a literal.
4. **Text — LAST resort → `COMPOSE_BY_TEXT` / `ESPRESSO_BY_TEXT` / `UIAUTOMATOR_WITH_TEXT`.** Only
   when the element exposes no tag/id/content-desc. Always `value = getStringResource(R.string.x)` so
   it survives localization — never hard-code English. Text is the most fragile handle (copy changes,
   duplicate strings, substring collisions).

How to read the source: `Modifier.testTag(x)` / `semantics { testTag = x }` → tag (strategy 1);
`Text(stringResource(id))` with no tag on that node → you're stuck with text (strategy 4) for it;
icon/image with a `contentDescription` → content-desc (strategy 3). Fenix mixes View / Compose /
GeckoView, so different elements on one screen may need different strategies — decide per element.

### Worked example: onboarding cards (why text is sometimes correct)

The onboarding card _titles_ are `Text(stringResource(title))` with **no** `testTag` or id on the
title node — so `COMPOSE_BY_TEXT` on the string resource is the right (and only) stable handle for
"is card X shown". The card _buttons_ do expose a tag, but it's title-prefixed and awkward
(`testTag = <localized title> + "onboarding_card.positive_button"`), so matching their text resource
(`nova_onboarding_continue_button`) is simpler and equally stable. Lesson: **prefer tags/ids, but
confirm what the composable actually exposes** — don't assume a tag exists, and don't reflexively
copy the legacy robot's text matcher when a tag _does_ exist.

## Groups (important)

Groups are how the harness works with sets of selectors:

- **`requiredForPage`** — the element(s) that prove you're on the screen. `navigateToPage()` polls
  these to confirm arrival, so pick something always present and stable, or navigation will flake.
- **`requiresScroll`** — element is off-screen until scrolled; the harness scrolls to it.
- **Domain groups** (e.g. `"aboutFirefox"`, `"jumpBackIn"`) — let a test assert a whole section with
  `mozVerifyElementsByGroup("...")` instead of listing each selector.

## Gotchas

- Every selector must be in `all`, or `mozGetSelectorsByGroup` won't see it.
- Don't over-scope `requiredForPage` to a volatile element (e.g. dynamic list content); use a stable
  title/container.
- Reuse a shared selector (like a common Continue button) rather than duplicating it per card/screen.
