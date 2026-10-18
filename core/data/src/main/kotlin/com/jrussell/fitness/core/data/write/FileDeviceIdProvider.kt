package com.jrussell.fitness.core.data.write

import com.jrussell.fitness.core.model.ids.DeviceId
import java.io.File
import java.io.IOException
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps the device ID in a file in [directory]. `:app` passes `noBackupFilesDir`, so the ID is
 * never restored from a backup: a restore onto another phone (or after a reinstall) gets a fresh
 * ID, and two devices never share a sync tie-breaker. A missing or unreadable file gets a new
 * random ID, as does one that can't be read (an I/O error); if the new ID can't be written it is kept
 * in memory for this process. The value is cached after the first read.
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
        val stored: Uuid? = readStored(file)
        if (stored != null) {
            return DeviceId(stored)
        }
        val created: Uuid = Uuid.random()
        try {
            directory.mkdirs()
            file.writeText(created.toString())
        } catch (
            // Unwritable storage: the ID still works for this process (it is cached), and the next
            // launch tries again. A sync tie-breaker that changes once is harmless.
            @Suppress("SwallowedException") unwritable: IOException,
        ) {
            // Kept in memory only; see above.
        }
        return DeviceId(created)
    }

    /** The stored ID, or `null` when the file is missing, unreadable (an I/O error) or not a UUID. */
    private fun readStored(file: File): Uuid? {
        return try {
            if (file.isFile) Uuid.parseOrNull(file.readText().trim()) else null
        } catch (
            @Suppress("SwallowedException") unreadable: IOException,
        ) {
            null
        }
    }

    private companion object {
        const val FILE_NAME: String = "device_id"
    }
}
