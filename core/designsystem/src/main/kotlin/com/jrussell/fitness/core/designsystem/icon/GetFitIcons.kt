package com.jrussell.fitness.core.designsystem.icon

import androidx.annotation.DrawableRes
import com.jrussell.fitness.core.designsystem.R

/**
 * The app's icons: Material Symbols Outlined, 24 dp, from google/material-design-icons. Tabs
 * have a filled `…Selected` variant for the selected state. Draw them with
 * `Icon(painterResource(id), ...)`; their color comes from `LocalContentColor`.
 */
object GetFitIcons {
    @DrawableRes val Home: Int = R.drawable.ic_home

    @DrawableRes val HomeSelected: Int = R.drawable.ic_home_selected

    @DrawableRes val Nutrition: Int = R.drawable.ic_nutrition

    @DrawableRes val NutritionSelected: Int = R.drawable.ic_nutrition_selected

    @DrawableRes val Training: Int = R.drawable.ic_training

    @DrawableRes val TrainingSelected: Int = R.drawable.ic_training_selected

    @DrawableRes val Body: Int = R.drawable.ic_body

    @DrawableRes val BodySelected: Int = R.drawable.ic_body_selected

    @DrawableRes val Settings: Int = R.drawable.ic_settings

    @DrawableRes val Back: Int = R.drawable.ic_back

    @DrawableRes val Check: Int = R.drawable.ic_check

    @DrawableRes val Delete: Int = R.drawable.ic_delete

    /** Every icon with its name, for tests and previews. */
    val all: List<Pair<String, Int>> = listOf(
        "Home" to Home,
        "HomeSelected" to HomeSelected,
        "Nutrition" to Nutrition,
        "NutritionSelected" to NutritionSelected,
        "Training" to Training,
        "TrainingSelected" to TrainingSelected,
        "Body" to Body,
        "BodySelected" to BodySelected,
        "Settings" to Settings,
        "Back" to Back,
        "Check" to Check,
        "Delete" to Delete,
    )
}
