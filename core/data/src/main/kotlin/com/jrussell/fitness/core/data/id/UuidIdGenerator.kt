package com.jrussell.fitness.core.data.id

import com.jrussell.fitness.core.domain.IdGenerator
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * [IdGenerator] backed by `kotlin.uuid`: UUIDv7 timestamps come from the injected [clock], so
 * tests are deterministic; UUIDv5 IDs are derived in [APP_NAMESPACE].
 */
class UuidIdGenerator @Inject constructor(private val clock: Clock) : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun newTimeOrderedId(): Uuid {
        return Uuid.generateV7NonMonotonicAt(clock.now())
    }

    override fun nameBasedId(name: String): Uuid {
        return uuidV5(APP_NAMESPACE, name)
    }

    companion object {
        /**
         * GetFit's UUIDv5 namespace. Never change it: copied reference foods and built-in rows
         * derive their IDs from it, and those IDs must match across devices and versions.
         */
        val APP_NAMESPACE: Uuid = Uuid.parse("3f6b2c1e-8a4d-4f7a-9c2e-5b1d7e9a0c44")
    }
}

private const val VERSION_BYTE: Int = 6
private const val VARIANT_BYTE: Int = 8
private const val LOW_NIBBLE_MASK: Int = 0x0F
private const val VERSION_5_BITS: Int = 0x50
private const val LOW_SIX_BITS_MASK: Int = 0x3F
private const val RFC_VARIANT_BITS: Int = 0x80

/** RFC 9562 UUIDv5: SHA-1 of the namespace bytes followed by the UTF-8 name. */
internal fun uuidV5(namespace: Uuid, name: String): Uuid {
    val digest: MessageDigest = MessageDigest.getInstance("SHA-1")
    digest.update(namespace.toByteArray())
    digest.update(name.toByteArray(Charsets.UTF_8))
    val bytes: ByteArray = digest.digest().copyOf(Uuid.SIZE_BYTES)
    bytes[VERSION_BYTE] = ((bytes[VERSION_BYTE].toInt() and LOW_NIBBLE_MASK) or VERSION_5_BITS).toByte()
    bytes[VARIANT_BYTE] = ((bytes[VARIANT_BYTE].toInt() and LOW_SIX_BITS_MASK) or RFC_VARIANT_BITS).toByte()
    return Uuid.fromByteArray(bytes)
}
