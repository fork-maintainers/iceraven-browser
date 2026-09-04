/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.conventions

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencySubstitution
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Bundling
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.logging.Logger
import org.gradle.api.logging.StandardOutputListener
import org.gradle.api.plugins.AppliedPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestOutputListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

class ProjectPlugin : Plugin<Project> {
    @Suppress("UNCHECKED_CAST")
    override fun apply(project: Project) {
        val mozilla = project.extensions.create("mozilla", ProjectExtension::class.java)
        mozilla.androidComponentsProject.convention(false)
        mozilla.ktlintSourcePaths.convention(emptyList())
        mozilla.detektSourcePaths.convention(emptyList())
        mozilla.detektAutoCorrect.convention(true)
        mozilla.detektReports.convention(emptyMap())

        val extraProperties = project.gradle.extensions.extraProperties
        val mozconfig = extraProperties["mozconfig"] as Map<String, Any>
        val topsrcdir = mozconfig["topsrcdir"] as String
        val topobjdir = mozconfig["topobjdir"] as String
        val substs = mozconfig["substs"] as Map<String, Any>
        val configureMavenRepositories = extraProperties["configureMavenRepositories"] as groovy.lang.Closure<*>

        project.repositories.apply {
            configureMavenRepositories.call(this)
            maven { setUrl("${topobjdir}/gradle/maven") }
        }

        configureBuildDirectory(project, topsrcdir, topobjdir)
        configureJniKeepDebugSymbols(project)
        configureKotlinCompilerMessageReformatting(project)
        configureKotlinWarningsAsErrors(project)
        configureAndroidBuildToolsVersion(project, substs)
        configureKotlinJvmToolchain(project)
        configureAppServicesSubstitution(project, extraProperties, substs)
        configureGleanSubstitution(project, extraProperties)
        configureGleanVersionResolution(project)
        configureKtlint(project, mozilla)
        configureDetekt(project, mozilla)
        configureAndroidComponentsLint(project, mozilla, topsrcdir)
        configureTestOutputFormatting(project)
        configurePackagingResourcesExcludes(project)
        registerPrintVariantsTask(project)
    }

    // Initialize the project buildDir to be in ${topobjdir} to follow
    // conventions of mozilla-central build system.
    private fun configureBuildDirectory(project: Project, topsrcdir: String, topobjdir: String) {
        val topSrcPath = File(topsrcdir).toPath()
        val topObjPath = File(topobjdir).toPath()

        val sourcePath = project.buildFile.toPath().parent
        val relativePath = topSrcPath.relativize(sourcePath)

        if (relativePath.startsWith("..")) {
            // The project doesn't appear to be in topsrcdir so leave the
            // buildDir alone.
        } else {
            // Transplant the project path into "${topobjdir}/gradle/build".
            // This is consistent with existing gradle / taskcluster
            // configurations but less consistent with the result of the
            // non-gradle build system.
            project.layout.buildDirectory.set(topObjPath.resolve("gradle/build").resolve(relativePath).toFile())
        }
    }

    // This explicitly disables stripping of native libraries in our projects to match the existing
    // implicit behaviour. Our projects do not specify the `ndkVersion` for our main Android builds
    // and so stripping would otherwise fail with a warning. Note that gecko builds themselves will
    // already strip the *.so files when compiled as release targets.
    @Suppress("UNCHECKED_CAST")
    private fun configureJniKeepDebugSymbols(project: Project) {
        val action = Action<AppliedPlugin> {
            val android = project.extensions.getByName("android")
            val packaging = android.javaClass.getMethod("getPackaging").invoke(android)
            val jniLibs = packaging.javaClass.getMethod("getJniLibs").invoke(packaging)
            val keepDebugSymbols = jniLibs.javaClass.getMethod("getKeepDebugSymbols").invoke(jniLibs)
            (keepDebugSymbols as MutableSet<String>).add("**/*.so")
        }
        project.pluginManager.withPlugin("com.android.library", action)
        project.pluginManager.withPlugin("com.android.application", action)
    }

    // Querying whether :geckoview is part of the build composition; no project-isolation-safe alternative.
    @Suppress("GradleProjectIsolation")
    private fun configureAppServicesSubstitution(
        project: Project,
        extraProperties: org.gradle.api.plugins.ExtraPropertiesExtension,
        substs: Map<String, Any>,
    ) {
        // Only substitute when the a-s subprojects are part of this Gradle build:
        // either :geckoview is included (so the a-s subprojects are too), or we're
        // downloading every Gradle dependency. When m/a/fenix builds on its own
        // (e.g., the second pass of a fat-AAR build), :geckoview isn't in settings
        // and fenix consumes a-s as Maven AARs from target.maven.zip instead of
        // maven.mozilla.org.
        if (substs["MOZ_APPSERVICES_IN_TREE"].isTruthy() && (substs["DOWNLOAD_ALL_GRADLE_DEPENDENCIES"].isTruthy() || project.findProject(":geckoview") != null)) {
            // In tree, so we update our legacy "external" dep name to a local project.
            // e.g., "org.mozilla.appservices:syncmanager:X.Y.Z" becomes project(':syncmanager')
            substituteDependencies(project, APP_SERVICES_GROUPS) { group, module, dependency ->
                var name = module
                // full-megazord-libsForTests is a convenience; we don't lose test coverage,
                // just local test convenience.
                if (name == "full-megazord-libsForTests") {
                    name = "full-megazord"
                }
                dependency.useTarget(project.project(":$name"))
            }
        } else if (extraProperties.has("localProperties.autoPublish.application-services.dir")) {
            substituteWithMavenLocal(project, "local-appservices", APP_SERVICES_GROUPS, "org.mozilla.appservices")
        }
    }

    private fun configureGleanSubstitution(
        project: Project,
        extraProperties: org.gradle.api.plugins.ExtraPropertiesExtension
    ) {
        if (extraProperties.has("localProperties.autoPublish.glean.dir")) {
            substituteWithMavenLocal(project, "local-glean", GLEAN_GROUPS)
        }
    }

    // Substitutes dependencies to use locally published versions from mavenLocal.
    private fun substituteWithMavenLocal(
        project: Project,
        tag: String,
        groups: Set<String>,
        targetGroup: String? = null
    ) {
        project.logger.lifecycle("[$tag] adjusting $project to use locally published modules ($LOCAL_SNAPSHOT_VERSION)")
        project.repositories.mavenLocal()
        substituteDependencies(project, groups) { group, module, dependency ->
            dependency.useTarget(
                mapOf(
                    "group" to (targetGroup ?: group),
                    "name" to module,
                    "version" to LOCAL_SNAPSHOT_VERSION
                )
            )
        }
    }

    private fun substituteDependencies(
        project: Project,
        groups: Set<String>,
        action: (group: String, module: String, dependency: DependencySubstitution) -> Unit
    ) {
        project.configurations.configureEach(object : Action<Configuration> {
            override fun execute(config: Configuration) {
                if (config.isCanBeResolved) {
                    config.resolutionStrategy.dependencySubstitution.all(object : Action<DependencySubstitution> {
                        override fun execute(dependency: DependencySubstitution) {
                            val requested = dependency.requested
                            if (requested is ModuleComponentSelector && requested.group in groups) {
                                action(requested.group, requested.module, dependency)
                            }
                        }
                    })
                }
            }
        })
    }

    private fun configureKotlinCompilerMessageReformatting(project: Project) {
        // Kotlin compiler message formats:
        // - Current: "e: file.kt:10:5 message" (colon-separated, used by fenix/focus/A-C)
        // - Legacy:  "e: file.kt: (10, 5): message" (parenthesized, used by geckoview)
        val messageFormats = listOf(
            Regex("""([ew]): (.+):(\d+):(\d+) (.*)"""),
            Regex("""([ew]): (.+): \((\d+), (\d+)\): (.*)"""),
        )

        project.tasks.configureEach {
            if (!this::class.java.name.startsWith("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")) {
                return@configureEach
            }

            // Translate Kotlin messages like "w: ..." and "e: ..." into
            // "...: warning: ..." and "...: error: ...", to make Treeherder understand.
            val listener = StandardOutputListener { message ->
                if (message.startsWith("e: warnings found")) {
                    return@StandardOutputListener
                }

                if (message.startsWith("w: ") || message.startsWith("e: ")) {
                    val match = messageFormats.firstNotNullOfOrNull { it.find(message) }
                    if (match == null) {
                        logger.quiet("kotlinc message format has changed!")
                        // For warnings, don't continue because we don't want to throw an
                        // exception. For errors, we want the exception so that the new error
                        // message format gets translated properly.
                        if (message.startsWith("w: ")) {
                            return@StandardOutputListener
                        }
                    }
                    match?.let {
                        val (type, file, line, column, msg) = it.destructured
                        val level = if (type == "w") "warning" else "error"
                        // Use logger.lifecycle, which does not go through stderr again.
                        logger.lifecycle("$file:$line:$column: $level: $msg")
                    }
                }
            }

            doFirst {
                logging.addStandardErrorListener(listener)
            }
            doLast {
                logging.removeStandardErrorListener(listener)
            }
        }
    }

    private fun configureKotlinWarningsAsErrors(project: Project) {
        project.tasks.configureEach {
            if (!this::class.java.name.startsWith("org.jetbrains.kotlin.gradle.tasks.KotlinCompile")) {
                return@configureEach
            }
            val compilerOptions = this::class.java.getMethod("getCompilerOptions").invoke(this)
            val allWarningsAsErrors = compilerOptions::class.java.getMethod("getAllWarningsAsErrors").invoke(compilerOptions)
            allWarningsAsErrors::class.java.getMethod("set", Any::class.java).invoke(allWarningsAsErrors, true)
        }
    }

    private fun configureAndroidBuildToolsVersion(project: Project, substs: Map<String, Any>) {
        val buildToolsVersion = substs["ANDROID_BUILD_TOOLS_VERSION"] as String

        // Use android plugin id string and reflection to avoid classloader isolation issues
        project.pluginManager.withPlugin("com.android.base") {
            val android = project.extensions.findByName("android") ?: return@withPlugin
            android::class.java.getMethod("setBuildToolsVersion", String::class.java)
                .invoke(android, buildToolsVersion)
        }
    }

    // Reads rootProject extra properties; IsolatedProject does not expose extensions/extraProperties.
    @Suppress("GradleProjectIsolation")
    private fun configureKotlinJvmToolchain(project: Project) {
        // Wait for Android plugin first to ensure Java plugin extension exists
        project.pluginManager.withPlugin("com.android.base") {
            project.pluginManager.withPlugin("org.jetbrains.kotlin.android") {
                val kotlin = project.extensions.findByName("kotlin") ?: return@withPlugin
                val config = project.rootProject.extensions.extraProperties["config"] ?: return@withPlugin
                val jvmTargetCompatibility = config.javaClass.getField("jvmTargetCompatibility").get(config) as Int
                kotlin::class.java.getMethod("jvmToolchain", Integer.TYPE)
                    .invoke(kotlin, jvmTargetCompatibility)
            }
        }
    }

    private fun configureGleanVersionResolution(project: Project) {
        // Dependencies can't depend on a different major version of Glean than A-C itself.
        val action = Action<AppliedPlugin> {
            val versionCatalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
            val libs = versionCatalogs.named("libs")
            val gleanVersion = libs.findVersion("glean").get().requiredVersion

            project.configurations.configureEach {
                resolutionStrategy {
                    eachDependency {
                        if (requested.group == "org.mozilla.telemetry" && requested.name.contains("glean")) {
                            val requestedMajor = requested.version?.split(".")?.firstOrNull()
                            val definedMajor = gleanVersion.split(".").firstOrNull()
                            // Check the major version
                            if (requestedMajor != definedMajor) {
                                throw AssertionError(
                                    "Cannot resolve to a single Glean version. " +
                                        "Requested: ${requested.version}, version catalog defines: $gleanVersion"
                                )
                            } else {
                                // Enforce that all (transitive) dependencies are using the defined Glean version
                                useVersion(gleanVersion)
                            }
                        }
                    }
                    capabilitiesResolution {
                        withCapability("org.mozilla.telemetry:glean-native") {
                            val toBeSelected = candidates.find {
                                it.id is ModuleComponentIdentifier &&
                                    (it.id as ModuleComponentIdentifier).module.contains("geckoview")
                            }
                            if (toBeSelected != null) {
                                select(toBeSelected)
                            }
                            because("use GeckoView Glean instead of standalone Glean")
                        }
                    }
                }
            }
        }
        project.pluginManager.withPlugin("com.android.library", action)
        project.pluginManager.withPlugin("com.android.application", action)
    }

    companion object {
        private const val LOCAL_SNAPSHOT_VERSION = "0.0.1-SNAPSHOT-+"
        private val APP_SERVICES_GROUPS = setOf("org.mozilla.appservices", "org.mozilla.appservices.nightly")
        private val GLEAN_GROUPS = setOf("org.mozilla.telemetry")
    }

    private fun configureKtlint(project: Project, mozilla: ProjectExtension) {
        val sourcePaths = mozilla.ktlintSourcePaths

        val ktlintConfig = project.configurations.register("ktlint")

        val ktlintDep = project.provider {
            val versionCatalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
            val libs = versionCatalogs.named("libs")
            val dep = project.dependencies.create(libs.findLibrary("ktlint").get().get())
            if (dep is ExternalModuleDependency) {
                dep.attributes {
                    attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
                }
            }
            dep
        }
        ktlintConfig.configure { dependencies.addLater(ktlintDep) }
        val ktlintClasspath = project.files(ktlintConfig)

        // Resolve the include/exclude globs (with leading "!" meaning exclude)
        // into a FileTree rooted at projectDir, so Gradle can use the actual
        // Kotlin source set to compute UP-TO-DATE / build cache keys.
        fun ktlintSourceTree() = project.fileTree(project.projectDir).matching {
            sourcePaths.get().forEach { pattern ->
                if (pattern.startsWith("!")) {
                    exclude(pattern.removePrefix("!"))
                } else {
                    include(pattern)
                }
            }
        }

        project.tasks.register("ktlint", JavaExec::class.java) {
            group = "verification"
            description = "Check Kotlin code style."
            classpath = ktlintClasspath
            mainClass.set("com.pinterest.ktlint.Main")
            onlyIf { sourcePaths.get().isNotEmpty() }
            sourcePaths.get().forEach { args(it) }
            args("--reporter=json,output=build/reports/ktlint/ktlint.json")
            args("--reporter=plain")
            inputs.files(ktlintSourceTree())
                .withPropertyName("ktlintSources")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
                .skipWhenEmpty()
            outputs.file(project.file("build/reports/ktlint/ktlint.json"))
                .withPropertyName("ktlintReport")
            outputs.cacheIf { true }
        }

        project.tasks.register("ktlintFormat", JavaExec::class.java) {
            group = "formatting"
            description = "Fix Kotlin code style deviations."
            classpath = ktlintClasspath
            mainClass.set("com.pinterest.ktlint.Main")
            onlyIf { sourcePaths.get().isNotEmpty() }
            args("-F")
            sourcePaths.get().forEach { args(it) }
            args("--reporter=json,output=build/reports/ktlint/ktlintFormat.json")
            args("--reporter=plain")
            jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
            inputs.files(ktlintSourceTree())
                .withPropertyName("ktlintFormatSources")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
                .skipWhenEmpty()
            outputs.file(project.file("build/reports/ktlint/ktlintFormat.json"))
                .withPropertyName("ktlintFormatReport")
        }
    }

    private fun configureDetekt(project: Project, mozilla: ProjectExtension) {
        val sourcePaths = mozilla.detektSourcePaths

        val detektConfig = project.configurations.register("detektCli")
        val detektDep = project.provider {
            val versionCatalogs = project.extensions.getByType(VersionCatalogsExtension::class.java)
            val libs = versionCatalogs.named("libs")
            project.dependencies.create(libs.findLibrary("detekt-cli").get().get())
        }
        detektConfig.configure { dependencies.addLater(detektDep) }
        val detektClasspath = project.files(detektConfig)

        // Subproject's build.gradle can add dependencies like
        // detektPlugins project(":components:tooling-detekt")
        val detektPlugins = project.configurations.register("detektPlugins") {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        val detektPluginFiles = project.files(detektPlugins)

        val projectDir = project.projectDir

        fun JavaExec.configureCommon() {
            classpath = detektClasspath
            mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
            // Resolve the include/exclude globs (with leading "!" meaning exclude)
            // into a FileTree rooted at projectDir, so Gradle can use the actual
            // Kotlin source set to compute UP-TO-DATE / build cache keys.
            val detektSourceTree =
                if (sourcePaths.get().none { !it.startsWith("!") }) {
                    project.files()
                } else {
                    project.fileTree(projectDir).matching {
                        sourcePaths.get().forEach { pattern ->
                            if (pattern.startsWith("!")) {
                                exclude(pattern.removePrefix("!"))
                            } else {
                                include(pattern)
                            }
                        }
                    }
                }
            onlyIf { !detektSourceTree.isEmpty }

            mozilla.detektConfig.orNull?.let {
                val file = project.file(it)
                args("--config", file.absolutePath)
                inputs.file(file).withPropertyName("detektConfig")
            }

            inputs.files(detektSourceTree)
                .withPropertyName("detektSources")
                .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
                .skipWhenEmpty()
            inputs.files(detektPluginFiles)
                .withPropertyName("detektPlugins")
                .optional(true)

            val includes = sourcePaths.get().filter { !it.startsWith("!") }
            val excludes = sourcePaths.get().filter { it.startsWith("!") }.map {it.removePrefix("!") }
            args("--input", projectDir.absolutePath)
            if (includes.isNotEmpty()) args("--includes", includes.joinToString(","))
            if (excludes.isNotEmpty()) args("--excludes", excludes.joinToString(","))

            argumentProviders.add(CommandLineArgumentProvider {
                val plugins = detektPluginFiles.files.filter { it.exists() }
                if (plugins.isNotEmpty()) {
                    listOf("--plugins", plugins.joinToString(",") { it.absolutePath })
                } else {
                    emptyList()
                }
            })
        }

        project.tasks.register("detekt", JavaExec::class.java) {
            group = "verification"
            description = "Run detekt static analysis."
            configureCommon()

            if (mozilla.detektAutoCorrect.get()) {
                args("--auto-correct")
                jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
            }

            mozilla.detektBaseline.orNull?.let {
                val file = project.file(it)
                if (file.exists()) {
                    args("--baseline", file.absolutePath)
                    inputs.file(file).withPropertyName("detektBaseline").optional(true)
                }
            }
            mozilla.detektReports.get().forEach { (id, path) -> args("--report", "$id:$path") }
            mozilla.detektReports.get().values.forEach { outputs.file(project.file(it)) }
            outputs.cacheIf { true }
        }

        project.tasks.register("detektBaseline", JavaExec::class.java) {
            group = "verification"
            description = "Regenerate the detekt baseline."
            configureCommon()
            val baselineFile = mozilla.detektBaseline.orNull?.let { project.file(it) }
            onlyIf { baselineFile != null }
            args("--create-baseline")
            baselineFile?.let { args("--baseline", it.absolutePath) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun configureAndroidComponentsLint(project: Project, mozilla: ProjectExtension, topsrcdir: String) {
        val action = Action<AppliedPlugin> {
            if (!mozilla.androidComponentsProject.get()) {
                return@Action
            }
            val sarifOutput = File(
                topsrcdir,
                "android-components/build/reports/lint/lint-report-${project.name}.sarif.json",
            )
            val android = project.extensions.getByName("android")
            val lint = android.javaClass.getMethod("getLint").invoke(android)
            lint.javaClass.getMethod("setBaseline", File::class.java)
                .invoke(lint, project.file("lint-baseline.xml"))
            lint.javaClass.getMethod("setWarningsAsErrors", Boolean::class.javaPrimitiveType)
                .invoke(lint, true)
            lint.javaClass.getMethod("setAbortOnError", Boolean::class.javaPrimitiveType)
                .invoke(lint, false)
            lint.javaClass.getMethod("setSarifReport", Boolean::class.javaPrimitiveType)
                .invoke(lint, true)
            lint.javaClass.getMethod("setSarifOutput", File::class.java)
                .invoke(lint, sarifOutput)
            val disable = lint.javaClass.getMethod("getDisable").invoke(lint) as MutableSet<String>
            disable.addAll(
                listOf(
                    "MissingTranslation",
                    "ExtraTranslation",
                    "MissingDefaultResource",
                    // We do not want to enforce this as a generic rule for all languages (see #6117, #6056, #6118)
                    "TypographyEllipsis",
                    // https://github.com/mozilla-mobile/android-components/issues/10641
                    "UnspecifiedImmutableFlag",
                    // https://bugzilla.mozilla.org/show_bug.cgi?id=1795427
                    "UnusedResources",
                    // "We do not impose rules on locales"
                    // https://github.com/mozilla-mobile/android-components/pull/11069
                    "TypographyDashes",
                ),
            )
        }
        project.pluginManager.withPlugin("com.android.library", action)
        project.pluginManager.withPlugin("com.android.application", action)
    }

    // Translates JUnit test events into Mozilla's TBPL-like textual format that Taskcluster
    // log parsing expects. See also: testing/mozbase/mozlog/mozlog/formatters/tbplformatter.py
    private fun configureTestOutputFormatting(project: Project) {
        project.pluginManager.withPlugin("com.android.base") {
            project.tasks.withType(Test::class.java).configureEach {
                systemProperty("robolectric.logging", "stdout")
                systemProperty("logging.test-mode", "true")
                systemProperty("javax.net.ssl.trustStoreType", "JKS")

                testLogging.events = emptySet()

                val listener = MozillaTestOutputListener(logger)
                addTestListener(listener)
                addTestOutputListener(listener)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun configurePackagingResourcesExcludes(project: Project) {
        val action = Action<AppliedPlugin> {
            val android = project.extensions.getByName("android")
            val packaging = android.javaClass.getMethod("getPackaging").invoke(android)
            val resources = packaging.javaClass.getMethod("getResources").invoke(packaging)
            val excludes = resources.javaClass.getMethod("getExcludes").invoke(resources) as MutableSet<String>
            excludes.addAll(listOf("META-INF/LICENSE.md", "META-INF/LICENSE-notice.md"))
        }
        project.pluginManager.withPlugin("com.android.library", action)
        project.pluginManager.withPlugin("com.android.application", action)
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerPrintVariantsTask(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            val outputFile = project.file("build/printVariants.json")
            val variants = mutableListOf<Map<String, Any?>>()

            // Only collect variant metadata when printVariants is actually being run. onVariants is a
            // configuration-time callback (unlike the legacy applicationVariants collection, which we could
            // wrap in a lazy provider resolved at execution), so gating its registration keeps the
            // reflection out of every other build that configures this project.
            val wantsPrintVariants = project.gradle.startParameter.taskNames.any {
                it.substringAfterLast(':') == "printVariants"
            }
            if (wantsPrintVariants) {
                val androidComponents = project.extensions.getByName("androidComponents")
                val android = project.extensions.getByName("android")
                val buildTypes = android.javaClass.getMethod("getBuildTypes").invoke(android)

                // Collect variant metadata via the new androidComponents.onVariants API (the legacy
                // applicationVariants API is removed in AGP 9). We go through reflection because this
                // convention plugin has no runtime dependency on the Android Gradle plugin; its types are
                // loaded from the consuming project's classloader. The APK file name is reconstructed from
                // the flavor/ABI/build type since the new VariantOutput no longer exposes the output file.
                val collectVariant = { variant: Any ->
                    val variantName = variant.javaClass.getMethod("getName").invoke(variant) as String
                    val buildType = variant.javaClass.getMethod("getBuildType").invoke(variant) as String?
                    val flavorName = variant.javaClass.getMethod("getFlavorName").invoke(variant) as String?
                    val flavorPrefix = if (!flavorName.isNullOrEmpty()) "$flavorName-" else ""
                    // AGP appends "-unsigned" to APKs whose build type has no signing config.
                    val signed = buildTypes.javaClass.getMethod("getByName", String::class.java)
                        .invoke(buildTypes, buildType)
                        .let { it.javaClass.getMethod("getSigningConfig").invoke(it) != null }
                    val signedSuffix = if (signed) "" else "-unsigned"
                    val outputs = variant.javaClass.getMethod("getOutputs").invoke(variant) as Iterable<*>

                    val apks = outputs.map { output ->
                        val filters = output!!.javaClass.getMethod("getFilters").invoke(output) as Iterable<*>
                        val abi = filters
                            .firstOrNull { f ->
                                val filterType = f!!.javaClass.getMethod("getFilterType").invoke(f)
                                (filterType as Enum<*>).name == "ABI"
                            }
                            ?.let { f -> f!!.javaClass.getMethod("getIdentifier").invoke(f) as String }
                            ?: "universal"
                        mapOf("abi" to abi, "fileName" to "app-$flavorPrefix$abi-$buildType$signedSuffix.apk")
                    }.sortedBy { it["abi"] as String }
                    variants.add(mapOf("apks" to apks, "build_type" to buildType, "name" to variantName))
                }

                val selector = androidComponents.javaClass.getMethod("selector").invoke(androidComponents)
                val allSelector = selector.javaClass.getMethod("all").invoke(selector)
                // onVariants(VariantSelector, (Variant) -> Unit). The callback is a Kotlin function type;
                // build the proxy from the method's own parameter type so it is loaded by the same
                // (Android Gradle plugin) classloader the method expects.
                val onVariants = androidComponents.javaClass.methods.first {
                    it.name == "onVariants" &&
                        it.parameterCount == 2 &&
                        it.parameterTypes[1].name == "kotlin.jvm.functions.Function1"
                }
                val callbackType = onVariants.parameterTypes[1]
                val callback = java.lang.reflect.Proxy.newProxyInstance(
                    callbackType.classLoader,
                    arrayOf(callbackType),
                ) { _, method, args ->
                    when (method.name) {
                        "invoke" -> { collectVariant(args!![0]!!); Unit }
                        "equals" -> false
                        "hashCode" -> 0
                        "toString" -> "printVariantsCollector"
                        else -> null
                    }
                }
                onVariants.invoke(androidComponents, allSelector, callback)
            }

            project.tasks.register("printVariants") {
                outputs.file(outputFile)

                doLast {
                    val variantsList = variants.toMutableList()
                    variantsList.add(
                        mapOf(
                            "apks" to listOf(
                                mapOf(
                                    "abi" to "noarch",
                                    "fileName" to "app-debug-androidTest.apk"
                                )
                            ),
                            "build_type" to "androidTest",
                            "name" to "androidTest"
                        )
                    )
                    outputFile.parentFile.mkdirs()
                    outputFile.writeText(groovy.json.JsonOutput.toJson(variantsList))
                    logger.debug("Wrote variant info to $outputFile")
                }
            }
        }
    }
}

private class MozillaTestOutputListener(
    private val taskLogger: Logger,
) : TestListener, TestOutputListener {
    override fun beforeSuite(suite: TestDescriptor) {
        if (suite.className != null) {
            println("\nSUITE: ${suite.className}")
        }
    }

    override fun afterSuite(suite: TestDescriptor, result: TestResult) {}

    override fun beforeTest(testDescriptor: TestDescriptor) {
        println("  TEST: ${testDescriptor.name}")
    }

    override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
        when (result.resultType) {
            TestResult.ResultType.SUCCESS -> println("  SUCCESS")
            TestResult.ResultType.FAILURE -> {
                val testId = "${testDescriptor.className}.${testDescriptor.name}"
                println("  TEST-UNEXPECTED-FAIL | $testId | ${result.exception}")
            }
            TestResult.ResultType.SKIPPED -> println("  SKIPPED")
        }
        taskLogger.lifecycle("")
    }

    override fun onOutput(testDescriptor: TestDescriptor, outputEvent: TestOutputEvent) {
        taskLogger.lifecycle("    ${outputEvent.message.trim()}")
    }
}
