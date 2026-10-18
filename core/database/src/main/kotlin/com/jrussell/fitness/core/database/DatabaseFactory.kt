package com.jrussell.fitness.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

/** File name of the user database in the app's `databases/` directory. */
const val USER_DATABASE_NAME: String = "user.db"

/**
 * The schema version of `user.db`. Pinned from `v0.1.0`: changing the schema means bumping this,
 * adding a numbered migration with a `MigrationTestHelper` test, and adding the new version's
 * hash to [USER_DATABASE_IDENTITY_HASHES] (`SchemaJsonTest` enforces it).
 */
const val USER_DATABASE_VERSION: Int = 1

/**
 * Room's identity hash for every schema version that has shipped, oldest first. Entries are only
 * ever added: a snapshot restore accepts any version listed here (Room migrates it on open) and
 * rejects a file whose hash doesn't match its version.
 */
val USER_DATABASE_IDENTITY_HASHES: Map<Int, String> = mapOf(
    1 to "0cf6a91b7d0c77f034d4018880010478",
)

/**
 * Opens `user.db`. Create exactly one per process (`:app` provides it as a singleton).
 *
 * @param queryContext where Room runs queries; `:app` passes the injected IO dispatcher.
 * @param corruptDirectory where a corrupt database file is copied before SQLite deletes it
 *   ([CorruptionPreservingCallback]).
 * @param clock dates the preserved copy; `:app` passes the injected clock.
 */
fun createGetFitDatabase(
    context: Context,
    queryContext: CoroutineContext,
    corruptDirectory: File,
    clock: Clock,
): GetFitDatabase {
    return Room.databaseBuilder(context, GetFitDatabase::class.java, USER_DATABASE_NAME)
        .setQueryCoroutineContext(queryContext)
        .openHelperFactory(
            CorruptionPreservingOpenHelperFactory(corruptDirectory, clock, FrameworkSQLiteOpenHelperFactory()),
        )
        .build()
}
