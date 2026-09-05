package com.jetbrains.python.envs

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.process.ExecOperations

class PythonEnvsPlugin implements Plugin<Project> {
    private static String osName = System.getProperty('os.name').replaceAll(' ', '').with {
        return it.contains("Windows") ? "Windows" : it
    }

    private static Boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
    private static Boolean isUnix = Os.isFamily(Os.FAMILY_UNIX)
    private static Boolean isMacOsX = Os.isFamily(Os.FAMILY_MAC)

    private static final String PIP_MINIMAL_SUPPORTED_VERSION = "3.9"

    private static URL getUrlToDownloadConda(Conda conda) {
        final String repository = (conda.version.toLowerCase().contains("miniconda")) ? "miniconda" : "archive"
        final String arch = getArch()
        final String ext = isWindows ? "exe" : "sh"

        return new URL("https://repo.continuum.io/$repository/${conda.version}-$osName-$arch.$ext")
    }

    private static String getArch() {
        def arch = System.getProperty("os.arch")
        switch (arch) {
            case ~/x86|i386|ia-32|i686/:
                arch = "x86"
                break
            case ~/x86_64|amd64|x64|x86-64/:
                arch = "x86_64"
                break
            case ~/arm|arm-v7|armv7|arm32/:
                arch = "armv7l"
                break
            case ~/aarch64|arm64|arm-v8/:
                arch = isMacOsX ? "arm64" : "aarch64"
                break
        }
        return arch
    }

    private static File getExecutable(String executable, Python env = null, File dir = null, EnvType type = null) {
        String pathString

        switch (type ?: env.type) {
            case [EnvType.PYTHON, EnvType.CONDA]:
                if (executable in ["pip", "virtualenv", "conda"]) {
                    pathString = isWindows ? "Scripts/${executable}.exe" : "bin/${executable}"
                } else if (executable.startsWith("python")) {
                    pathString = isWindows ? "${executable}.exe" : "bin/${executable}"
                } else {
                    throw new RuntimeException("$executable is not supported for $env.type yet")
                }
                break
            case [EnvType.JYTHON, EnvType.PYPY]:
                if (env.type == EnvType.JYTHON && executable == "python") executable = "jython"
                pathString = "bin/${executable}${isWindows ? '.exe' : ''}"
                break
            case EnvType.IRONPYTHON:
                if (executable in ["ipy", "python"] ) {
                    pathString = "net45/${env.is64 ? "ipy.exe" : "ipy32.exe"}"
                } else {
                    pathString = "Scripts/${executable}.exe"
                }
                break
            case EnvType.VIRTUALENV:
                pathString = isWindows ? "Scripts/${executable}.exe" : "bin/${executable}"
                break
            default:
                throw new RuntimeException("$env.type env type is not supported yet")
        }

        return new File(dir ?: env.envDir, pathString)
    }


    @Override
    void apply(Project project) {
        PythonEnvsExtension envs = project.extensions.create("envs", PythonEnvsExtension.class)

        def buildCondas = project.tasks.register("build_condas") {
            group = "build"
            description = "Builds all configured conda environments"
            onlyIf { !envs.condas.empty }
        }

        project.afterEvaluate {
            envs.condas.each { Conda env ->
                String taskName = "Bootstrap_${env.type}_${env.name}"

                def bootstrapTask = project.tasks.register(taskName) {
                    group = "setup"

                    onlyIf {
                        !env.envDir.exists() || isPythonInvalid(project, env)
                    }

                    def execOps = project.services.get(ExecOperations)

                    doFirst {
                        File bDir = project.layout.buildDirectory.asFile.get()
                        if (!bDir.exists()) bDir.mkdirs()
                        env.envDir.mkdirs()
                        env.envDir.deleteDir()
                    }

                    doLast {
                        URL urlToConda = getUrlToDownloadConda(env)
                        File installer = new File(project.layout.buildDirectory.asFile.get(), urlToConda.toString().split("/").last())

                        if (!installer.exists()) {
                            project.logger.quiet("Downloading ${installer.name}")
                            project.ant.get(dest: installer) {
                                url(url: urlToConda)
                            }
                        }

                        project.logger.quiet("Bootstrapping to ${env.envDir}")
                        execOps.exec {
                            if (isWindows) {
                                commandLine installer, "/InstallationType=JustMe", "/AddToPath=0", "/RegisterPython=0", "/S", "/D=${env.envDir}"
                            } else {
                                if (env.envDir.exists()) {
                                    sleep(30 * 1000)
                                }
                                commandLine "bash", installer, "-b", "-u", "-p", env.envDir
                            }
                        }

                        pipInstall(project, env, env.packages, execOps)
                        condaInstall(project, env, env.condaPackages, execOps)
                    }
                }

                buildCondas.configure {
                    dependsOn bootstrapTask
                }
            }
        }

        project.tasks.register('build_envs') {
            group = "build"
            dependsOn buildCondas
        }
    }

    private void pipInstall(Project project, Python env, List<String> packages, ExecOperations execOps) {
        if (packages == null || packages.empty || env.type == null) {
            return
        }
        project.logger.quiet("Installing packages via pip: $packages")

        List<String> command = [
                getExecutable("pip", env),
                "install",
                *project.extensions.findByName("envs").getProperty("pipInstallOptions").split(" "),
                *packages
        ]
        project.logger.quiet("Executing '${command.join(" ")}'")

        if (execOps.exec {
            commandLine command
        }.exitValue != 0) throw new GradleException("pip install failed")
    }

    private void condaInstall(Project project, Conda conda, List<String> packages, ExecOperations execOps) {
        if (packages == null || packages.empty) {
            return
        }
        project.logger.quiet("Installing packages via conda: $packages")

        List<String> command = [
                getExecutable("conda", conda),
                "install", "-y",
                "-p", conda.envDir,
                *packages
        ]
        project.logger.quiet("Executing '${command.join(" ")}'")

        if (execOps.exec {
            commandLine command
        }.exitValue != 0) throw new GradleException("conda install failed")
    }

    private boolean isPythonValid(Project project, Python env) {
        File exec = getExecutable("python", env)
        if (!exec.exists()) return false

        int exitValue
        try {
            exitValue = execOps.exec { commandLine exec, "-c", "'print(1)'" }.exitValue
        } catch (ignored) {
            return false
        }

        return exitValue == 0
    }

    private boolean isPythonInvalid(Project project, Python env) {
        return !isPythonValid(project, env)
    }
}
