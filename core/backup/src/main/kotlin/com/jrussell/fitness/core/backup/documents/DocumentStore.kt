package com.jrussell.fitness.core.backup.documents

import java.io.InputStream
import java.io.OutputStream

/**
 * Files the user chose through the system picker (Storage Access Framework), by URI string. Every
 * method reports a missing document, a revoked permission or an unreachable provider as `null` or
 * `false` rather than throwing, so callers can turn it into a message.
 */
interface DocumentStore {
    /** The folder's or file's display name, e.g. "GetFit backups". */
    fun displayName(uri: String): String?

    /** Keeps read and write access to the folder [treeUri] across restarts. */
    fun takePersistablePermission(treeUri: String): Boolean

    /** Whether access to [treeUri] is still held. */
    fun hasPermission(treeUri: String): Boolean

    fun openInput(uri: String): InputStream?

    /** Opens [uri] for writing, replacing its contents. */
    fun openOutput(uri: String): OutputStream?

    /** Creates a file called [name] in the folder [treeUri]; returns its URI. */
    fun createFile(treeUri: String, name: String, mimeType: String): String?

    /** The folder's files, by display name to URI; `null` when the folder can't be reached. */
    fun listNames(treeUri: String): Map<String, String>?

    fun delete(uri: String): Boolean
}
