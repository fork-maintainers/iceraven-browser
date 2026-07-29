/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.espresso.Espresso
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import java.io.ByteArrayOutputStream

/**
 * ScreenDump — a concise, greppable snapshot of what the Compose UI currently exposes.
 *
 * When a selector-based step fails, the single most useful debugging artifact is a tight list of the
 * elements actually on screen and the handles they expose. This prints ONE line per interesting node
 * with exactly the fields that decide a selector: text, testTag, contentDescription, and whether it's
 * clickable. From that you can tell at a glance whether the element the test wanted is present
 * (selector is wrong → fix the selector) or absent (the screen isn't what the test expected → fix the
 * test/navigation).
 *
 * Lives in `helpers/` (not `devtools/`) on purpose: BasePage calls it automatically on moz* failures,
 * and core must not depend on devtools.
 *
 * Output is bracketed by BEGIN/END markers under the logcat tag "EffScreenDump", so a whole run
 * collapses to one grep. To capture just the dump(s) after a failing run:
 *
 *   adb logcat -c            # clear first (before running the test)
 *   # ...run the test...
 *   adb logcat -d -s EffScreenDump:I
 *
 * BaseTest/BasePage fire this automatically when a mozVerify/mozClick step fails, so usually you just
 * rerun and grep. You can also call it manually for exploration: ScreenDump.dump(composeRule, "label").
 */
object ScreenDump {

    const val TAG = "EffScreenDump"
    const val BEGIN = "EFF_SCREEN_DUMP:BEGIN"
    const val END = "EFF_SCREEN_DUMP:END"

    fun dump(composeRule: AndroidComposeTestRule<*, *>, label: String = "") {
        Log.i(TAG, "$BEGIN ${label.ifBlank { "(no label)" }}")
        try {
            val nodes = composeRule
                .onAllNodes(SemanticsMatcher("all nodes") { true }, useUnmergedTree = true)
                .fetchSemanticsNodes()

            var shown = 0
            nodes.forEach { node ->
                val cfg = node.config
                val text = cfg.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
                val tag = cfg.getOrNull(SemanticsProperties.TestTag)
                val desc = cfg.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
                // Only print nodes carrying a usable handle — keeps the dump tight.
                if (text.isNullOrBlank() && tag.isNullOrBlank() && desc.isNullOrBlank()) return@forEach
                val clickable = if (cfg.contains(SemanticsActions.OnClick)) " [clickable]" else ""
                val parts = buildList {
                    if (!text.isNullOrBlank()) add("text=\"$text\"")
                    if (!tag.isNullOrBlank()) add("testTag=\"$tag\"")
                    if (!desc.isNullOrBlank()) add("desc=\"$desc\"")
                }
                Log.i(TAG, "• ${parts.joinToString("  ")}$clickable")
                shown++
            }
            Log.i(TAG, "($shown nodes with a text/testTag/desc handle, of ${nodes.size} total)")
        } catch (t: Throwable) {
            Log.i(TAG, "screen dump unavailable: ${t.message}")
        }
        Log.i(TAG, END)
    }

    /**
     * Native / View-hierarchy snapshot via UIAutomator — complements [dump] (which only sees Compose
     * semantics). Prints one line per interesting View with the handles that decide a UIAUTOMATOR_* /
     * ESPRESSO_* selector: res-id (package prefix stripped, matching UIAUTOMATOR_WITH_RES_ID values),
     * text, content-description, and clickability. Needed for legacy View screens (RecyclerViews, res-id
     * widgets) that don't appear in the Compose tree.
     */
    fun dumpUiAutomator(label: String = "") {
        Log.i(TAG, "$BEGIN [uiautomator] ${label.ifBlank { "(no label)" }}")
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val xml = ByteArrayOutputStream().use { baos ->
                device.dumpWindowHierarchy(baos)
                baos.toString("UTF-8")
            }
            var shown = 0
            Regex("<node\\b[^>]*>").findAll(xml).forEach { m ->
                val node = m.value
                fun attr(a: String) = Regex("$a=\"([^\"]*)\"").find(node)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                val resId = attr("resource-id")?.substringAfterLast("/")
                val text = attr("text")
                val desc = attr("content-desc")
                if (resId == null && text == null && desc == null) return@forEach
                val clickable = if (attr("clickable") == "true") " [clickable]" else ""
                val parts = buildList {
                    if (resId != null) add("res-id=\"$resId\"")
                    if (text != null) add("text=\"$text\"")
                    if (desc != null) add("desc=\"$desc\"")
                }
                Log.i(TAG, "• ${parts.joinToString("  ")}$clickable")
                shown++
            }
            Log.i(TAG, "($shown native nodes with a res-id/text/desc handle)")
        } catch (t: Throwable) {
            Log.i(TAG, "uiautomator dump unavailable: ${t.message}")
        }
        Log.i(TAG, END)
    }

    /**
     * Espresso / in-process View-hierarchy snapshot. Complements [dumpUiAutomator]: Espresso sees the full
     * View tree of the foreground activity — including views that carry no accessibility handle (which the
     * UIAutomator dump omits) — and the handles Espresso can match that UIAutomator can't expose: the concrete
     * View class, `hint`, and the parent/child nesting (shown by indentation). Prefer ESPRESSO_BY_ID for a
     * res-id on a View (framework priority); use this to find the id/class/hint to feed it.
     */
    fun dumpEspresso(label: String = "") {
        Log.i(TAG, "$BEGIN [espresso] ${label.ifBlank { "(no label)" }}")
        try {
            Espresso.onView(ViewMatchers.isRoot()).perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> = Matchers.any(View::class.java)
                override fun getDescription() = "eff dump espresso view hierarchy"
                override fun perform(uiController: UiController, view: View) = walk(view, 0)
            })
        } catch (t: Throwable) {
            Log.i(TAG, "espresso dump unavailable: ${t.message}")
        }
        Log.i(TAG, END)
    }

    private fun walk(view: View, depth: Int) {
        val id = if (view.id != View.NO_ID) {
            runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull()
        } else {
            null
        }
        val text = (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }
        val hint = (view as? TextView)?.hint?.toString()?.takeIf { it.isNotBlank() }
        val desc = view.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        // Print a line only for views carrying an actionable handle (id/text/hint/desc); skip pure layout wrappers.
        if (id != null || text != null || hint != null || desc != null) {
            val parts = buildList {
                if (id != null) add("id=\"$id\"")
                add("class=${view.javaClass.simpleName}")
                if (text != null) add("text=\"$text\"")
                if (hint != null) add("hint=\"$hint\"")
                if (desc != null) add("desc=\"$desc\"")
                if (view.isClickable) add("[clickable]")
                // Espresso walks the tree regardless of visibility; flag the ones a selector can't match
                // on screen so they aren't mistaken for usable handles.
                when (view.visibility) {
                    View.GONE -> add("[gone]")
                    View.INVISIBLE -> add("[invisible]")
                }
            }
            Log.i(TAG, "• ${"  ".repeat(depth)}${parts.joinToString("  ")}")
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), depth + 1)
        }
    }
}
