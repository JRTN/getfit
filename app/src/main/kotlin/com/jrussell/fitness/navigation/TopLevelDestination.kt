package com.jrussell.fitness.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import com.jrussell.fitness.feature.body.BodyKey
import com.jrussell.fitness.feature.body.R as BodyR
import com.jrussell.fitness.feature.nutrition.NutritionKey
import com.jrussell.fitness.feature.nutrition.R as NutritionR
import com.jrussell.fitness.feature.today.R as TodayR
import com.jrussell.fitness.feature.today.TodayKey
import com.jrussell.fitness.feature.training.R as TrainingR
import com.jrussell.fitness.feature.training.TrainingKey
import com.jrussell.fitness.feature.trends.R as TrendsR
import com.jrussell.fitness.feature.trends.TrendsKey

/**
 * The five bottom-bar tabs, in display order. Icons are M0 placeholders from
 * material-icons-core; proper Material Symbols arrive with the M1 design system.
 */
enum class TopLevelDestination(val key: NavKey, @param:StringRes val label: Int, val icon: ImageVector) {
    TODAY(TodayKey, TodayR.string.feature_today_title, Icons.Filled.Home),
    NUTRITION(NutritionKey, NutritionR.string.feature_nutrition_title, Icons.AutoMirrored.Filled.List),
    TRAINING(TrainingKey, TrainingR.string.feature_training_title, Icons.Filled.PlayArrow),
    BODY(BodyKey, BodyR.string.feature_body_title, Icons.Filled.Person),
    TRENDS(TrendsKey, TrendsR.string.feature_trends_title, Icons.Filled.DateRange),
    ;

    companion object {
        /**
         * The tab whose root is [key].
         *
         * @throws NoSuchElementException if [key] is not a tab root — a programming error.
         */
        fun forKey(key: NavKey): TopLevelDestination {
            return entries.first { destination: TopLevelDestination -> destination.key == key }
        }
    }
}
