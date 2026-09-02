/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.helpers

import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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
        // A single Compose dump is not enough on a mixed-layer app: system dialogs, other-package
        // windows (e.g. the stylus-handwriting prompt), the soft keyboard, and legacy Views all live
        // OUTSIDE the Compose semantics tree and are the actual cause of many "element not found"
        // failures. So on every failure we emit ALL layers plus a window/focus summary — this is the
        // reliable "did an overlay/popup steal focus or cover my target?" signal.
        dumpWindows(label)
        dumpUiAutomator(label)
        dumpEspresso(label)
    }

    /**
     * Window + input-focus summary. Answers the question the element dumps alone can't: is a window
     * OTHER than the app's activity on top right now (a system dialog / popup / soft keyboard), and
     * which element currently holds input focus? When a selector "isn't found", this tells you whether
     * an overlay covered it or stole focus — versus the element genuinely not being on the screen.
     */
    fun dumpWindows(label: String = "") {
        Log.i(TAG, "$BEGIN [windows] ${label.ifBlank { "(no label)" }}")
        try {
            val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
            val windows = ui.windows
            Log.i(TAG, "windows on screen: ${windows.size}")
            windows.forEach { w ->
                val type = when (w.type) {
                    AccessibilityWindowInfo.TYPE_APPLICATION -> "APPLICATION"
                    AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "INPUT_METHOD(keyboard)"
                    AccessibilityWindowInfo.TYPE_SYSTEM -> "SYSTEM"
                    AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "A11Y_OVERLAY"
                    AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "SPLIT_DIVIDER"
                    else -> "type=${w.type}"
                }
                val flags = buildList {
                    if (w.isActive) add("active")
                    if (w.isFocused) add("focused")
                }.joinToString(",")
                Log.i(TAG, "• window title=\"${w.title}\" type=$type${if (flags.isNotBlank()) " [$flags]" else ""}")
            }
            val nonApp = windows.filter { it.type != AccessibilityWindowInfo.TYPE_APPLICATION }
            if (nonApp.isNotEmpty()) {
                Log.i(
                    TAG,
                    "⚠ non-application window(s) present (possible overlay/popup/keyboard over the app): " +
                        nonApp.joinToString { "\"${it.title}\" (${it.type})" },
                )
            }
            val focused = ui.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null) {
                Log.i(
                    TAG,
                    "focused input: pkg=${focused.packageName} id=${focused.viewIdResourceName} " +
                        "text=\"${focused.text}\" desc=\"${focused.contentDescription}\"",
                )
            } else {
                Log.i(TAG, "focused input: (none)")
            }
        } catch (t: Throwable) {
            Log.i(TAG, "windows dump unavailable: ${t.message}")
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

    /**
     * Full-screen snapshot for the interactive inspector + selector overlay. Captures a screenshot PLUS
     * every element across all three layers (Compose semantics, Espresso view tree, UIAutomator native
     * tree) WITH on-screen bounds and a suggested selector. Writes to the app's INTERNAL files dir, so the
     * artifacts are retrievable with `adb shell run-as` (exact commands are logged under [TAG]):
     *   eff-screendump.png   — full-screen screenshot
     *   eff-screendump.json  — { screen, screenshot, elements:[ {layer, bounds:[l,t,r,b], handles, strategy, value} ] }
     * Pull both and render them in one step with the effview tool:
     *   devtools/effview/effview.sh --no-trigger
     *
     * Pass the test's composeRule to include the Compose layer (testTags); omit for View-only screens.
     * ALIGNMENT NOTE: UIAutomator + Espresso bounds are full-screen; Compose uses boundsInWindow. If the
     * Compose boxes are offset by the status-bar height on your device, subtract the window top offset
     * (logged) when calibrating.
     */
    fun dumpAll(composeRule: AndroidComposeTestRule<*, *>? = null, label: String = "") {
        val instr = InstrumentationRegistry.getInstrumentation()
        val pkg = instr.targetContext.packageName
        val filesDir = instr.targetContext.filesDir // INTERNAL storage — retrievable via `adb shell run-as`
        Log.i(TAG, "$BEGIN [all] ${label.ifBlank { "(no label)" }}")
        val elements = JSONArray()
        var w = 0
        var h = 0
        var pngOk = false
        try {
            val bmp = instr.uiAutomation.takeScreenshot()
            if (bmp == null) {
                Log.i(TAG, "takeScreenshot() returned null (no screenshot this run)")
            } else {
                w = bmp.width; h = bmp.height
                FileOutputStream(File(filesDir, "eff-screendump.png")).use {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                pngOk = true
            }
        } catch (t: Throwable) {
            Log.i(TAG, "screenshot unavailable: ${t.message}")
        }

        // Compose semantics layer (testTags live here)
        if (composeRule != null) {
            try {
                composeRule.onAllNodes(SemanticsMatcher("all") { true }, useUnmergedTree = true)
                    .fetchSemanticsNodes().forEach { node ->
                        val cfg = node.config
                        val tag = cfg.getOrNull(SemanticsProperties.TestTag)
                        val text = cfg.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
                        val desc = cfg.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" ")
                        if (tag.isNullOrBlank() && text.isNullOrBlank() && desc.isNullOrBlank()) return@forEach
                        val b = node.boundsInWindow
                        val (strat, value) = when {
                            !tag.isNullOrBlank() -> "COMPOSE_BY_TAG" to tag
                            !desc.isNullOrBlank() -> "COMPOSE content-description" to desc
                            else -> "COMPOSE_BY_TEXT (last resort)" to (text ?: "")
                        }
                        elements.put(
                            JSONObject()
                                .put("layer", "compose")
                                .put("bounds", JSONArray(listOf(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())))
                                .put("testTag", tag ?: "").put("text", text ?: "").put("contentDesc", desc ?: "")
                                .put("clickable", cfg.contains(SemanticsActions.OnClick))
                                .put("strategy", strat).put("value", value),
                        )
                    }
            } catch (t: Throwable) {
                Log.i(TAG, "compose layer unavailable: ${t.message}")
            }
        }

        // UIAutomator native layer
        try {
            val device = UiDevice.getInstance(instr)
            val xml = ByteArrayOutputStream().use { device.dumpWindowHierarchy(it); it.toString("UTF-8") }
            Regex("<node\\b[^>]*>").findAll(xml).forEach { m ->
                val node = m.value
                fun attr(a: String) = Regex("$a=\"([^\"]*)\"").find(node)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
                val rid = attr("resource-id"); val text = attr("text"); val desc = attr("content-desc")
                if (rid == null && text == null && desc == null) return@forEach
                val bm = Regex("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").find(attr("bounds") ?: "") ?: return@forEach
                val (l, t2, r, b2) = bm.destructured
                val (strat, value) = when {
                    rid != null && rid.contains(":id/") -> "UIAUTOMATOR_WITH_RES_ID / ESPRESSO_BY_ID" to rid.substringAfterLast("/")
                    rid != null -> "raw-resourceId strategy" to rid
                    desc != null -> "UIAUTOMATOR_WITH_CONTENT_DESC" to desc
                    else -> "*_BY_TEXT (last resort)" to (text ?: "")
                }
                elements.put(
                    JSONObject()
                        .put("layer", "uiautomator")
                        .put("bounds", JSONArray(listOf(l.toInt(), t2.toInt(), r.toInt(), b2.toInt())))
                        .put("resId", rid ?: "").put("text", text ?: "").put("contentDesc", desc ?: "")
                        .put("clickable", attr("clickable") == "true")
                        .put("strategy", strat).put("value", value),
                )
            }
        } catch (t: Throwable) {
            Log.i(TAG, "uiautomator layer unavailable: ${t.message}")
        }

        // Espresso in-process view layer
        try {
            Espresso.onView(ViewMatchers.isRoot()).perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> = Matchers.any(View::class.java)
                override fun getDescription() = "eff dumpAll espresso"
                override fun perform(uiController: UiController, view: View) = collectViews(view, elements)
            })
        } catch (t: Throwable) {
            Log.i(TAG, "espresso layer unavailable: ${t.message}")
        }

        try {
            val root = JSONObject()
                .put("screen", JSONObject().put("width", w).put("height", h))
                .put("screenshot", "eff-screendump.png")
                .put("elements", elements)
            File(filesDir, "eff-screendump.json").writeText(root.toString())
            Log.i(TAG, "DONE: wrote ${elements.length()} elements to $filesDir")
            Log.i(TAG, "RETRIEVE (or just run devtools/effview/effview.sh --no-trigger):")
            Log.i(TAG, "  adb exec-out run-as $pkg cat files/eff-screendump.json > eff-screendump.json")
            if (pngOk) Log.i(TAG, "  adb exec-out run-as $pkg cat files/eff-screendump.png > eff-screendump.png")
        } catch (t: Throwable) {
            Log.i(TAG, "json emit failed: ${t.message}")
        }
        Log.i(TAG, END)
    }

    private fun collectViews(view: View, out: JSONArray) {
        val id = if (view.id != View.NO_ID) {
            runCatching { view.resources.getResourceEntryName(view.id) }.getOrNull()
        } else {
            null
        }
        val text = (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }
        val desc = view.contentDescription?.toString()?.takeIf { it.isNotBlank() }
        if (id != null || text != null || desc != null) {
            val loc = IntArray(2); view.getLocationOnScreen(loc)
            out.put(
                JSONObject()
                    .put("layer", "espresso")
                    .put("bounds", JSONArray(listOf(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)))
                    .put("resId", id ?: "").put("text", text ?: "").put("contentDesc", desc ?: "")
                    .put("class", view.javaClass.simpleName).put("clickable", view.isClickable)
                    .put(
                        "strategy",
                        if (id != null) {
                            "ESPRESSO_BY_ID"
                        } else if (desc != null) {
                            "content-description"
                        } else {
                            "*_BY_TEXT"
                        },
                    )
                    .put("value", id ?: desc ?: text ?: ""),
            )
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) collectViews(view.getChildAt(i), out)
        }
    }
}
