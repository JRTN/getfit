package com.jrussell.fitness.core.database.infrastructure

import assertk.assertThat
import assertk.assertions.containsExactly
import com.jrussell.fitness.core.database.GetFitDatabase
import com.jrussell.fitness.core.database.inMemoryGetFitDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncSequenceDaoTest {
    private val database: GetFitDatabase = inMemoryGetFitDatabase()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun incrementAndGet_startsAtOneAndIncrements() = runTest {
        val dao: SyncSequenceDao = database.syncSequenceDao()

        val revisions: List<Long> = listOf(dao.incrementAndGet(), dao.incrementAndGet(), dao.incrementAndGet())

        assertThat(revisions).containsExactly(1L, 2L, 3L)
    }
}
