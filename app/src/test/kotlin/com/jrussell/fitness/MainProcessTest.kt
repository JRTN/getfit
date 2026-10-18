package com.jrussell.fitness

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

/**
 * App startup (bootstrap, backup scheduling, and through the database the staged-restore install)
 * runs only in the main process. The `:restart` process that finishes a full restore must not
 * open `user.db` while the old main process is still alive.
 */
class MainProcessTest {
    @Test
    fun mainProcess_runsStartup() {
        assertThat(isMainProcess("com.jrussell.fitness", "com.jrussell.fitness")).isTrue()
    }

    @Test
    fun restartProcess_skipsStartup() {
        assertThat(isMainProcess("com.jrussell.fitness:restart", "com.jrussell.fitness")).isFalse()
    }

    @Test
    fun unknownProcessName_skipsStartup() {
        assertThat(isMainProcess(null, "com.jrussell.fitness")).isFalse()
    }
}
