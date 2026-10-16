package com.jrussell.fitness.buildlogic

import org.gradle.api.Project
import org.gradle.api.provider.Provider

/** `versionCode` used when git history is unavailable (source zip, no commits yet). */
const val FALLBACK_VERSION_CODE: Int = 1

/**
 * The number of commits reachable from `HEAD`, used as the app's `versionCode` so that every
 * build from a later commit installs over an earlier one.
 *
 * Runs `git rev-list --count HEAD` through Gradle's `providers.exec`, which keeps the
 * configuration cache valid (the output becomes a cache input). Falls back to
 * [FALLBACK_VERSION_CODE] if git is missing, the directory is not a repository, or there are
 * no commits. CI must check out full history (`fetch-depth: 0`) or the count will be wrong.
 */
fun Project.gitCommitCount(): Provider<Int> {
    val execution = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }
    return execution.standardOutput.asText.map { output: String ->
        output.trim().toIntOrNull() ?: FALLBACK_VERSION_CODE
    }
}
