package com.jrussell.fitness.buildlogic

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.kotlin.dsl.of
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/** `versionCode` used when git history is unavailable (source zip, no commits, no git). */
const val FALLBACK_VERSION_CODE: Int = 1

/**
 * The number of commits reachable from `HEAD`, used as the app's `versionCode` so that every
 * build from a later commit installs over an earlier one.
 *
 * Falls back to [FALLBACK_VERSION_CODE] when:
 * - the project root has no `.git` (a source zip), so git is not run at all and a repository in
 *   a parent directory can't lend its commit count;
 * - the `git` program can't be started (not installed);
 * - git fails or prints no number (no commits yet).
 *
 * CI must check out full history (`fetch-depth: 0`) or the count will be wrong.
 */
fun Project.gitCommitCount(): Int {
    if (!rootDir.resolve(".git").exists()) {
        return FALLBACK_VERSION_CODE
    }
    return providers.of(GitCommitCountValueSource::class) {
        parameters.repositoryDirectory.set(rootDir)
    }.get()
}

/**
 * Runs `git rev-list --count HEAD` as a configuration-cache input. A Gradle `ValueSource` is
 * used rather than `providers.exec` so that a failure to start git can be caught and turned into
 * the fallback; a failed `providers.exec` would also block the configuration cache from storing.
 */
abstract class GitCommitCountValueSource : ValueSource<Int, GitCommitCountValueSource.Parameters> {
    /** Inputs to the value source. */
    interface Parameters : ValueSourceParameters {
        /** The repository root to run git in. */
        val repositoryDirectory: DirectoryProperty
    }

    /** Gradle's process launcher, injected by Gradle. */
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): Int {
        val output: ByteArrayOutputStream = ByteArrayOutputStream()
        val count: Int? = runCatching {
            execOperations.exec {
                workingDir = parameters.repositoryDirectory.get().asFile
                commandLine("git", "rev-list", "--count", "HEAD")
                standardOutput = output
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
            output.toString().trim().toIntOrNull()
        }.getOrNull()
        return count ?: FALLBACK_VERSION_CODE
    }
}
