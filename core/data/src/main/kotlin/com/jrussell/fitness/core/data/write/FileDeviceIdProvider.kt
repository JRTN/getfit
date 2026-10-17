package com.jrussell.fitness.core.data.write

import com.jrussell.fitness.core.model.ids.DeviceId
import java.io.File
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps the device ID in a file in [directory]. `:app` passes `noBackupFilesDir`, so the ID is
 * never restored from a backup: a restore onto another phone (or after a reinstall) gets a fresh
 * ID, and two devices never share a sync tie-breaker. A missing or unreadable file gets a new
 * random ID. The value is cached after the first read.
 */
class FileDeviceIdProvider(private val directory: File, private val ioDispatcher: CoroutineDispatcher) :
    DeviceIdProvider {
    private val mutex: Mutex = Mutex()
    private var cached: DeviceId? = null

    override suspend fun deviceId(): DeviceId {
        return mutex.withLock {
            cached ?: withContext(ioDispatcher) { readOrCreate() }.also { id: DeviceId -> cached = id }
        }
    }

    private fun readOrCreate(): DeviceId {
        val file: File = File(directory, FILE_NAME)
        val stored: Uuid? = if (file.exists()) Uuid.parseOrNull(file.readText().trim()) else null
        if (stored != null) {
            return DeviceId(stored)
        }
        val created: Uuid = Uuid.random()
        directory.mkdirs()
        file.writeText(created.toString())
        return DeviceId(created)
    }

    private companion object {
        const val FILE_NAME: String = "device_id"
    }
}
