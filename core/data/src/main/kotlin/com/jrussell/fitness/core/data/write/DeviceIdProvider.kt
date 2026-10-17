package com.jrussell.fitness.core.data.write

import com.jrussell.fitness.core.model.ids.DeviceId

/** This install's identity, stamped on every write as `originDeviceId`. */
interface DeviceIdProvider {
    suspend fun deviceId(): DeviceId
}
