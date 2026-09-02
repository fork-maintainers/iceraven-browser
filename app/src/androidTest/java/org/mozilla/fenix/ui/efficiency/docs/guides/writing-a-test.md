# Writing an efficiency test

Once the building blocks exist, the test is a short, fluent description of _what_ to check. It
extends `BaseTest`, which owns the compose rule, retries, cleanup, and the mock web server.

## Structure

```kotlin
class OnboardingTest : BaseTest(skipOnboarding = false) {

    // TestRail link: https://mozilla.testrail.io/index.php?/cases/view/3349493
    @SmokeTest
    @Test
    fun verifyTheTermsOfUseOnboardingCardTest() {
        on.onboarding.navigateToPage()
            .mozVerify(OnboardingSelectors.TERMS_OF_USE_TITLE)
    }
}
```

- **`BaseTest(...)` options** configure the launch: `skipOnboarding` (default true),
  `isPocketEnabled`, `isRecentlyVisitedFeatureEnabled`, `isPageLoadTranslationsPromptEnabled`. Set
  the ones the scenario needs.
- **`on`** is the `PageContext` — `on.<page>` gives the typed page object.
- **`navigateToPage()`** routes + confirms arrival; `navigateToPage(url)` on `browserPage` loads a
  page. Chain `moz*` verbs off it.
- **Preserve the `// TestRail link:` comment and `@SmokeTest`** from the legacy test — coverage
  tooling joins on the TestRail id, and the smoke tag drives suite membership.

## Faithful-port discipline

Default policy is faithful port: reproduce the legacy test's assertions, don't "improve" it while
converting. If the legacy test is weak/over-broad, port it as-is and log a separate quality-backlog
item — mixing a rewrite into a conversion makes review harder and muddies parity.

## Composing steps

Use the `moz*` verbs (full list in `extending-basepage.md`): `mozClick`, `mozClickIfPresent`,
`mozEnterText`, `mozPressEnter`, `mozSwipeTo`, etc., and verifies `mozVerify`,
`mozVerifyElementAbsent`, `mozVerifyElementsByGroup`, `mozVerifyAnyContainsText`, … Each returns the
page, so chain them. Prefer `mozClickIfPresent` for genuinely conditional UI (e.g. an optional
interstitial card) rather than branching logic in the test.

## Close-out (order matters)

1. Run the test in isolation until green (atomic runner or its shard).
2. Confirm parity: every legacy assertion has an equivalent here.
3. Only THEN add the `replacedBy` marker to the legacy test method — but in the **same commit** as the
   conversion. Adding it before green inflates the conversion burndown with tests that don't actually
   pass; leaving it for a follow-up pass means the conversion lands looking unconverted.
4. If the moved/added test belongs to a CI-run shard package, update the Flank configs in the SAME
   diff (there are three: arm-experimental-api-tests.yml runs them; arm64-v8a.yml and
   arm64-v8a-detect-leaks.yml exclude them) — otherwise coverage silently changes.

## Gotchas

- Don't reach into internals or duplicate navigation the graph already knows — let `navigateToPage`
  route.
- Keep one behavior per test where the legacy test did; don't merge cases during a faithful port.
