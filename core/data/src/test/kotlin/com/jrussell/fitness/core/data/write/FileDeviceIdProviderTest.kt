package com.jrussell.fitness.core.data.write

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jrussell.fitness.core.model.ids.DeviceId
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileDeviceIdProviderTest {
    @get:Rule
    val temporaryFolder: TemporaryFolder = TemporaryFolder()

    @Test
    fun deviceId_isStableAcrossInstances() = runTest {
        val dispatcher: TestDispatcher = StandardTestDispatcher(testScheduler)

        val first: DeviceId = FileDeviceIdProvider(temporaryFolder.root, dispatcher).deviceId()
        val second: DeviceId = FileDeviceIdProvider(temporaryFolder.root, dispatcher).deviceId()

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun deviceId_corruptFile_isReplacedWithValidId() = runTest {
        val file: File = File(temporaryFolder.root, "device_id")
        file.writeText("not-a-uuid")

        val id: DeviceId = FileDeviceIdProvider(temporaryFolder.root, StandardTestDispatcher(testScheduler)).deviceId()

        assertThat(Uuid.parseOrNull(file.readText())).isNotNull().isEqualTo(id.value)
    }

    @Test
    fun readFails_isTreatedAsMissing() = runTest {
        // A directory where the file should be: reading and writing it both fail.
        File(temporaryFolder.root, "device_id").mkdirs()
        val provider: FileDeviceIdProvider =
            FileDeviceIdProvider(temporaryFolder.root, StandardTestDispatcher(testScheduler))

        val first: DeviceId = provider.deviceId()

        assertThat(provider.deviceId()).isEqualTo(first)
    }
}
