package com.jrussell.fitness.core.backup.documents

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/**
 * [DocumentStore] over the system's document providers (`DocumentsContract`), so it works with any
 * provider the user picks: local storage, Google Drive, and others.
 *
 * Providers fail in many ways (a revoked grant is a `SecurityException`, a deleted folder an
 * `IllegalArgumentException` or `FileNotFoundException`); each becomes `null` or `false`.
 */
class ContentResolverDocumentStore(context: Context) : DocumentStore {
    private val resolver: ContentResolver = context.contentResolver

    override fun displayName(uri: String): String? {
        val parsed: Uri = Uri.parse(uri)
        val document: Uri = if (DocumentsContract.isTreeUri(parsed)) treeDocument(parsed) else parsed
        return guarded {
            resolver.query(document, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor: Cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }
    }

    override fun takePersistablePermission(treeUri: String): Boolean {
        return guarded {
            resolver.takePersistableUriPermission(Uri.parse(treeUri), READ_WRITE)
            true
        } ?: false
    }

    override fun hasPermission(treeUri: String): Boolean {
        return resolver.persistedUriPermissions.any { permission ->
            permission.uri.toString() == treeUri && permission.isReadPermission && permission.isWritePermission
        }
    }

    override fun openInput(uri: String): InputStream? {
        return guarded { resolver.openInputStream(Uri.parse(uri)) }
    }

    override fun openOutput(uri: String): OutputStream? {
        // "wt" truncates, so a shorter file never keeps a stale tail of the old one.
        return guarded { resolver.openOutputStream(Uri.parse(uri), "wt") }
    }

    override fun createFile(treeUri: String, name: String, mimeType: String): String? {
        return guarded {
            DocumentsContract.createDocument(resolver, treeDocument(Uri.parse(treeUri)), mimeType, name)?.toString()
        }
    }

    override fun listNames(treeUri: String): Map<String, String>? {
        val tree: Uri = Uri.parse(treeUri)
        if (!hasPermission(treeUri)) {
            return null
        }
        val children: Uri = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getTreeDocumentId(tree),
        )
        val columns: Array<String> =
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return guarded {
            resolver.query(children, columns, null, null, null)?.use { cursor: Cursor ->
                buildMap {
                    while (cursor.moveToNext()) {
                        put(
                            cursor.getString(1),
                            DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)).toString(),
                        )
                    }
                }
            }
        }
    }

    override fun delete(uri: String): Boolean {
        return guarded { DocumentsContract.deleteDocument(resolver, Uri.parse(uri)) } ?: false
    }

    private fun treeDocument(tree: Uri): Uri {
        return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    }

    /** Runs [block], turning the ways a provider reports "not available" into `null`. */
    @Suppress("SwallowedException")
    private fun <T> guarded(block: () -> T?): T? {
        return try {
            block()
        } catch (denied: SecurityException) {
            null
        } catch (missing: FileNotFoundException) {
            null
        } catch (invalid: IllegalArgumentException) {
            null
        } catch (unsupported: UnsupportedOperationException) {
            null
        }
    }

    private companion object {
        const val READ_WRITE: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
