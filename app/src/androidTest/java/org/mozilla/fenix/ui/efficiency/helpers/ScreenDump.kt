/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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
}
