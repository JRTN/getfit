package com.jrussell.fitness.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.jrussell.fitness.core.designsystem.icon.GetFitIcons
import com.jrussell.fitness.feature.body.BodyKey
import com.jrussell.fitness.feature.body.R as BodyR
import com.jrussell.fitness.feature.home.HomeKey
import com.jrussell.fitness.feature.home.R as HomeR
import com.jrussell.fitness.feature.nutrition.NutritionKey
import com.jrussell.fitness.feature.nutrition.R as NutritionR
import com.jrussell.fitness.feature.training.R as TrainingR
import com.jrussell.fitness.feature.training.TrainingKey

/**
 * The four bottom-bar tabs, in display order, each with an outlined icon and a filled one for
 * the selected state.
 */
enum class TopLevelDestination(
    val key: NavKey,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int,
) {
    HOME(HomeKey, HomeR.string.feature_home_title, GetFitIcons.Home, GetFitIcons.HomeSelected),
    NUTRITION(
        NutritionKey,
        NutritionR.string.feature_nutrition_title,
        GetFitIcons.Nutrition,
        GetFitIcons.NutritionSelected,
    ),
    TRAINING(TrainingKey, TrainingR.string.feature_training_title, GetFitIcons.Training, GetFitIcons.TrainingSelected),
    BODY(BodyKey, BodyR.string.feature_body_title, GetFitIcons.Body, GetFitIcons.BodySelected),
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
