package com.jrussell.fitness.core.domain.metric

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test

class BucketingTest {
    @Test
    fun bucketize_day_isIdentity() {
        val daily: List<DataPoint> = listOf(point(2026, 10, 12, 80.0), point(2026, 10, 13, 81.0))

        assertThat(bucketize(daily, Bucket.DAY, Aggregation.LAST)).isEqualTo(daily)
    }

    @Test
    fun bucketize_week_last_usesLastDayOfIsoWeek() {
        val daily: List<DataPoint> = listOf(point(2026, 10, 12, 80.0), point(2026, 10, 15, 81.5))

        assertThat(bucketize(daily, Bucket.WEEK, Aggregation.LAST)).containsExactly(point(2026, 10, 12, 81.5))
    }

    @Test
    fun bucketize_month_mean() {
        val daily: List<DataPoint> =
            listOf(point(2026, 10, 2, 80.0), point(2026, 10, 20, 82.0), point(2026, 11, 3, 90.0))

        assertThat(bucketize(daily, Bucket.MONTH, Aggregation.MEAN))
            .containsExactly(point(2026, 10, 1, 81.0), point(2026, 11, 1, 90.0))
    }

    @Test
    fun bucketize_sumMinMax() {
        val daily: List<DataPoint> = listOf(point(2026, 10, 12, 1.0), point(2026, 10, 13, 3.0))

        assertThat(bucketize(daily, Bucket.WEEK, Aggregation.SUM)).containsExactly(point(2026, 10, 12, 4.0))
        assertThat(bucketize(daily, Bucket.WEEK, Aggregation.MIN)).containsExactly(point(2026, 10, 12, 1.0))
        assertThat(bucketize(daily, Bucket.WEEK, Aggregation.MAX)).containsExactly(point(2026, 10, 12, 3.0))
    }

    private fun point(year: Int, month: Int, day: Int, value: Double): DataPoint {
        return DataPoint(LocalDate(year, month, day), value, coverage = null)
    }
}
