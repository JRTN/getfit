package com.jrussell.fitness.core.database

import android.content.Context
import androidx.room.Room
import kotlin.coroutines.CoroutineContext

/** File name of the user database in the app's `databases/` directory. */
const val USER_DATABASE_NAME: String = "user.db"

/**
 * Opens `user.db`. Create exactly one per process (`:app` provides it as a singleton).
 *
 * @param queryContext where Room runs queries; `:app` passes the injected IO dispatcher.
 */
fun createGetFitDatabase(context: Context, queryContext: CoroutineContext): GetFitDatabase {
    return Room.databaseBuilder(context, GetFitDatabase::class.java, USER_DATABASE_NAME)
        .setQueryCoroutineContext(queryContext)
        .build()
}
