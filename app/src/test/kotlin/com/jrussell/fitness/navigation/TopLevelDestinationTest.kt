package com.jrussell.fitness.navigation

import androidx.navigation3.runtime.NavKey
import assertk.assertThat
import assertk.assertions.containsExactly
import com.jrussell.fitness.feature.body.BodyKey
import com.jrussell.fitness.feature.home.HomeKey
import com.jrussell.fitness.feature.nutrition.NutritionKey
import com.jrussell.fitness.feature.training.TrainingKey
import org.junit.Test

class TopLevelDestinationTest {
    @Test
    fun tabs_areHomeNutritionTrainingBody_inOrder() {
        val keys: List<NavKey> = TopLevelDestination.entries.map { destination: TopLevelDestination -> destination.key }

        assertThat(keys).containsExactly(HomeKey, NutritionKey, TrainingKey, BodyKey)
    }
}
