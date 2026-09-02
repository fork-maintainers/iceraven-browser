# Converting a legacy test (the workflow)

The repeatable process for porting a legacy `ui/` smoke test onto ui/efficiency. Work the gates in order —
navigation is the spine, so reach the screen before you try to interact or assert. At each gate you either
compose with what already exists or add the smallest missing block, then continue. The `guides/` cover the
mechanics; this is the sequence that ties them together. The `tooling.md` helpers make each gate faster but
are optional — you can do every step by hand.

**Policy: faithful-port-first.** Reproduce the legacy test's coverage; don't redesign behavior during a
conversion. If you improve something or hit a harness limitation, note it (a comment / the lessons log), don't
silently change scope.

## 1. Pick and understand the test

Read the legacy method: entry state, target screen(s), the ordered steps, and the assertions. Note its
TestRail id and whether it's `@Ignore`d. Check whether an efficiency test of that name already exists — don't
re-convert. _(Helper: `effscaffold <Class.method>` prints all of this in one shot.)_

Write down the intent as a template — entry state → target page(s) → steps → assertions — and drop the legacy
robot/DSL mechanics.

## 2. Navigation gate — can you reach every target screen?

For each screen the test touches, is there a page object with a working `navigateToPage` route?

- If yes, use it.
- If a screen isn't modeled, or a selector/edge is missing, build it — **but discover the element's real
  handles first** (dump the screen; never trust a stub locator). → `guides/discovering-selectors.md`, then
  `guides/creating-a-page-object.md`, `guides/authoring-selectors.md`, `guides/adding-navigation.md`.

## 3. Interaction gate — are the actions expressible?

Can you express each step with existing `moz*` verbs (`mozClick`, `mozEnterText`, …) or existing page helpers?
**Reuse first** — a lot of capability already exists (custom-tab launch, trust-panel state verify,
recently-closed screen, web-form submit + save-login prompt, settings→Home back-edge). If something's genuinely
missing, add the smallest general primitive/helper. → `guides/extending-basepage.md`.

## 4. Assertion gate — are the verifications expressible?

Can you express each assertion with the `mozVerify*` family (often `mozVerifyElementsByGroup("...")`)? If not,
add a verify primitive. → `guides/extending-basepage.md`.

## 5. Static pre-flight (before you burn a device build)

Sanity-check the code without compiling: do `R.string`/`R.id` resolve, does every page object have a real nav
path, are there any inline selectors, do the `moz*` verbs exist, is the test-class boilerplate present
(`private val mockWebServer get() = fenixTestRule.mockWebServer`, `TestAssetHelper` imports)? Fix everything
first. _(Helper: `effcheck …` catches all of these.)_

## 6. Write, run, verify

Compose the test method → `guides/writing-a-test.md`. Run it in isolation (its atomic runner or shard), read
the run, and iterate to green. Done means the **named** test actually ran, was **not** skipped, and its run is
0-failed — "green gradle, 0 failed" alone is not proof (a skipped test also shows 0 failed). A test that only
passes on a retry is flaky, not done. → `guides/debugging-tests.md`. _(Helpers: `effpretty` renders the run;
`effverify` is the done-gate.)_

## 7. Parity + close-out

Confirm the converted test covers the legacy assertions. If you deliberately omit a leg (e.g. the nav graph
has no stateful return edge yet), leave a short parity note in the test and log it as a harness gap — don't
drop it silently. **Then** annotate the legacy method with `@Converted(replacedBy = [...], bug = NNNNN,
since = "YYYY-MM")`, recording any dropped coverage in `notes`. The gate is **green** — annotate it in the
**same commit as the conversion**, not in a later pass; the conversion burndown keys off that marker, so a
conversion that lands without it reads as unconverted.

## 8. Land it

File the Bugzilla bug, then edit the bug title to prepend `Bug NNNNN - ` so it matches your commit subject.

**Make the bug block the tracking meta bug,
[2030727](https://bugzilla.mozilla.org/show_bug.cgi?id=2030727)** (`[meta] TAE - Migrate and remove legacy
tests`). It tracks the campaign through its `depends_on` list, so a conversion that never gets linked is
invisible to anyone reading the meta for status. Set it when you file, not afterwards. This applies to
**test-conversion bugs only** — tooling, docs and harness-hardening bugs stay off the meta, since it is
specifically about migrating and removing legacy tests.

Commit as `Bug NNNNN - [efficiency] Convert <Test>.<method> to ui/efficiency r=isabel_rios,aaronmt`. Keep
diffs atomic (one test or one capability each) unless reviewers ask for larger batches. Submit with
`moz-phab submit` (reviewers: `isabel_rios`, `aaronmt`), then add the `testing-exception-unchanged` tag in
the Phabricator web UI. _(When the agent tooling is available, the `efficiency-conversion-loop` skill
automates step 8's bug/commit paperwork; solo, do them by hand.)_

## 9. Feed back what you learned

If a shape recurs across many tests, it's a factory-generation candidate — flag it rather than hand-writing the
Nth near-identical test. Every assumption that turned out wrong → add it to the lessons log so the next
conversion is right the first time.
