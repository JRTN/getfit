package com.jrussell.fitness.core.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.jrussell.fitness.core.datastore.BackupSettingsDataSource
import com.jrussell.fitness.core.testing.FakeClock
import java.io.File
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupStartupTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    private val now: Instant = Instant.parse("2026-10-18T12:00:00Z")
    private val scheduler: RecordingScheduler = RecordingScheduler()

    @Test
    fun folderSet_ensuresScheduleAndAsksForCatchUp() = runTest {
        val settings: BackupSettingsDataSource = settingsIn(this.backgroundScope)
        settings.setFolder("content://tree/1", "Backups")
        settings.recordSuccess(1_000)

        BackupStartup(settings, scheduler, FakeClock(now)).run()

        assertThat(scheduler.ensured).isEqualTo(1)
        assertThat(scheduler.catchUps).containsExactly(Instant.fromEpochMilliseconds(1_000) to now)
    }

    @Test
    fun noFolder_schedulesNothing() = runTest {
        BackupStartup(settingsIn(this.backgroundScope), scheduler, FakeClock(now)).run()

        assertThat(scheduler.ensured).isEqualTo(0)
        assertThat(scheduler.catchUps).isEmpty()
    }

    private fun settingsIn(scope: kotlinx.coroutines.CoroutineScope): BackupSettingsDataSource {
        val file: File = File(temporaryFolder.root, "prefs.preferences_pb")
        return BackupSettingsDataSource(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
    }
}
