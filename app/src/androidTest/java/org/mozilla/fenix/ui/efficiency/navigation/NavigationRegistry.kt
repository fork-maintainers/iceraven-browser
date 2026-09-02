/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ui.efficiency.navigation

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File

object NavigationRegistry {
    private const val TAG = "NavigationRegistry"

    private val graph = mutableMapOf<String, MutableList<NavigationEdge>>()

    fun register(from: String, to: String, steps: List<NavigationStep>, launch: LaunchConfig? = null) {
        val edge = NavigationEdge(from, to, steps, launch)
        graph.getOrPut(from) { mutableListOf() }.add(edge)

        Log.i(TAG, "📌 Registered navigation: $from -> $to with ${steps.size} step(s)")
        steps.forEachIndexed { index, step ->
            Log.i(TAG, "   Step ${index + 1}: $step")
        }
    }

    /** The LaunchConfig declared on any edge leading INTO [page], if any. */
    fun launchConfigFor(page: String): LaunchConfig? =
        graph.values.flatten().firstOrNull { it.to == page && it.launch != null }?.launch

    fun findPath(from: String, to: String): List<NavigationStep>? {
        if (from == to) {
            val selfLoopEdge = graph[from]?.find { it.to == to }

            return selfLoopEdge?.steps ?: emptyList()
        }

        val queue = ArrayDeque<Pair<String, List<NavigationStep>>>()
        val visited = mutableSetOf<String>()

        queue.add(Pair(from, emptyList()))
        visited.add(from)

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()

            for (edge in graph[current].orEmpty()) {
                if (edge.to in visited) continue

                val newPath = path + edge.steps

                if (edge.to == to) return newPath

                visited.add(edge.to)
                queue.add(Pair(edge.to, newPath))
            }
        }

        return null
    }

    /**
     * Returns all registered page names found in the graph.
     */
    fun getAllPages(): Set<String> {
        return buildSet {
            addAll(graph.keys)
            graph.values.flatten().forEach { edge ->
                add(edge.from)
                add(edge.to)
            }
        }
    }

    /**
     * Finds all distinct simple paths from [from] to [to].
     *
     * "Simple" means a page cannot appear twice in the same path.
     * This prevents infinite loops in cyclic graphs.
     */
    fun findAllPaths(from: String, to: String): List<NavigationPath> {
        val results = mutableListOf<NavigationPath>()

        if (from !in getAllPages() || to !in getAllPages()) {
            return emptyList()
        }

        findAllPathsDfs(
            current = from,
            target = to,
            visited = linkedSetOf(from),
            edgePath = mutableListOf(),
            results = results,
        )

        return results
    }

    private fun findAllPathsDfs(
        current: String,
        target: String,
        visited: LinkedHashSet<String>,
        edgePath: MutableList<NavigationEdge>,
        results: MutableList<NavigationPath>,
    ) {
        if (current == target) {
            results.add(
                NavigationPath(
                    pages = buildPageSequence(edgePath, current),
                    edges = edgePath.toList(),
                ),
            )
            return
        }

        for (edge in graph[current].orEmpty()) {
            if (edge.to in visited) {
                continue
            }

            visited.add(edge.to)
            edgePath.add(edge)

            findAllPathsDfs(
                current = edge.to,
                target = target,
                visited = visited,
                edgePath = edgePath,
                results = results,
            )

            edgePath.removeAt(edgePath.lastIndex)
            visited.remove(edge.to)
        }
    }

    private fun buildPageSequence(edgePath: List<NavigationEdge>, terminalPage: String): List<String> {
        if (edgePath.isEmpty()) return listOf(terminalPage)

        val pages = mutableListOf<String>()
        pages.add(edgePath.first().from)
        edgePath.forEach { pages.add(it.to) }
        return pages
    }

    /**
     * Logs every distinct simple path between two pages.
     */
    fun logAllPaths(from: String, to: String) {
        val paths = findAllPaths(from, to)

        Log.i(TAG, "🧭 Distinct navigation paths from '$from' to '$to': ${paths.size}")

        if (paths.isEmpty()) {
            Log.i(TAG, "   No distinct paths found.")
            return
        }

        paths.forEachIndexed { index, path ->
            Log.i(TAG, "   Path ${index + 1}: ${path.pages.joinToString(" -> ")}")

            if (path.edges.isEmpty()) {
                Log.i(TAG, "      (same page / zero-step path)")
            } else {
                path.edges.forEachIndexed { edgeIndex, edge ->
                    Log.i(
                        TAG,
                        "      Edge ${edgeIndex + 1}: ${edge.from} -> ${edge.to} " +
                            "[${edge.steps.size} step(s)]",
                    )
                    edge.steps.forEachIndexed { stepIndex, step ->
                        Log.i(TAG, "         Step ${stepIndex + 1}: $step")
                    }
                }
            }
        }
    }

    /**
     * Logs a graph-wide summary of all distinct simple navigation paths.
     *
     * Useful before wiring this into navigateToPage.
     */
    fun logPathSummary() {
        val pages = getAllPages().sorted()
        var totalPaths = 0
        var pairCountWithPaths = 0

        Log.i(TAG, "📊 Navigation path summary")
        Log.i(TAG, "   Registered pages: ${pages.size}")
        Log.i(TAG, "   Registered edges: ${graph.values.sumOf { it.size }}")

        for (from in pages) {
            for (to in pages) {
                if (from == to) continue

                val paths = findAllPaths(from, to)
                if (paths.isNotEmpty()) {
                    pairCountWithPaths++
                    totalPaths += paths.size

                    Log.i(
                        TAG,
                        "   $from -> $to : ${paths.size} distinct path(s)",
                    )
                }
            }
        }

        Log.i(TAG, "   Reachable page pairs: $pairCountWithPaths")
        Log.i(TAG, "   Total distinct paths across graph: $totalPaths")
    }

    fun logGraph() {
        Log.i(TAG, "🧭 Current navigation graph:")
        for ((from, edges) in graph) {
            for (edge in edges) {
                Log.i(TAG, " - $from -> ${edge.to} [${edge.steps.size} step(s)]")
            }
        }
    }

    fun exportDotToFile(outputFile: File): File {
        outputFile.writeText(toDot())
        Log.i(TAG, "Wrote DOT graph to: ${outputFile.absolutePath}")
        return outputFile
    }

    fun toDot(): String {
        return buildString {
            appendLine("digraph NavigationRegistry {")
            appendLine("  rankdir=LR;")
            appendLine("  node [shape=box];")

            getAllPages().sorted().forEach { page ->
                appendLine("""  "${escapeDot(page)}";""")
            }

            graph.values.flatten().forEach { edge ->
                val attrs = if (edge.steps.isEmpty()) {
                    """label="0", style="dashed""""
                } else {
                    """label="${edge.steps.size}""""
                }

                appendLine(
                    """  "${escapeDot(edge.from)}" -> "${escapeDot(edge.to)}" [$attrs];""",
                )
            }

            appendLine("}")
        }
    }

    private fun escapeDot(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }
}

/**
 * Represents one distinct navigation path through the graph.
 */
data class NavigationPath(
    val pages: List<String>,
    val edges: List<NavigationEdge>,
) {
    val totalSteps: Int
        get() = edges.sumOf { it.steps.size }
}
