/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.tomlj.Toml
import org.tomlj.TomlTable
import java.io.File

// Parses the Cargo.lock and returns the version for the given package name, or null when the
// package is absent.
fun getRustVersionFor(cargoLock: File, packageName: String): String? {
    var version: String? = null
    val result = Toml.parse(cargoLock.readText())
    result.getArray("package")?.toList().orEmpty().forEach { entry ->
        val table = entry as TomlTable
        if (table.getString("name") == packageName) {
            if (version != null) {
                throw GradleException(
                    "Multiple versions for '$packageName' found." +
                        " Ensure '$packageName' is only included once."
                )
            }
            version = table.getString("version")
        }
    }
    return version
}

/**
 * Checks that the Glean version the version catalog declares is the version `Cargo.lock` resolves,
 * so that the Android and the Rust halves of Glean cannot drift apart.
 *
 * @property source the `Cargo.lock` to read the resolved Rust version from.
 * @property expectedVersion the version the version catalog declares.
 * @property outputFile a marker naming the verified version.
 */
abstract class VerifyGleanVersionTask : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:Input
    abstract val expectedVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun verifyGleanVersion() {
        val foundVersion = getRustVersionFor(source.get().asFile, "glean")
        if (expectedVersion.get() != foundVersion) {
            throw GradleException(
                "Mismatched Glean version, expected: '${expectedVersion.get()}'," +
                    " found '$foundVersion'"
            )
        }
        logger.lifecycle("verifyGleanVersion> expected version matches found version '$foundVersion'")

        outputFile.get().asFile.writeText("glean-$foundVersion")
    }
}