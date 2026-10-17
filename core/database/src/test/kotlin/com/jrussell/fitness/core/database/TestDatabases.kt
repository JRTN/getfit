package com.jrussell.fitness.core.database

import androidx.room.Room
import org.robolectric.RuntimeEnvironment

/** A fresh in-memory [GetFitDatabase] for one test; close it in `@After`. */
fun inMemoryGetFitDatabase(): GetFitDatabase {
    return Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), GetFitDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}
