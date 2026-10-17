package com.jrussell.fitness.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.jrussell.fitness.core.designsystem.icon.GetFitIcons
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
 * The five bottom-bar tabs, in display order, each with an outlined icon and a filled one for
 * the selected state.
 */
enum class TopLevelDestination(
    val key: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
) {
    TODAY(TodayKey, TodayR.string.feature_today_title, GetFitIcons.Today, GetFitIcons.TodaySelected),
    NUTRITION(
        NutritionKey,
        NutritionR.string.feature_nutrition_title,
        GetFitIcons.Nutrition,
        GetFitIcons.NutritionSelected,
    ),
    TRAINING(TrainingKey, TrainingR.string.feature_training_title, GetFitIcons.Training, GetFitIcons.TrainingSelected),
    BODY(BodyKey, BodyR.string.feature_body_title, GetFitIcons.Body, GetFitIcons.BodySelected),
    TRENDS(TrendsKey, TrendsR.string.feature_trends_title, GetFitIcons.Trends, GetFitIcons.TrendsSelected),
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
