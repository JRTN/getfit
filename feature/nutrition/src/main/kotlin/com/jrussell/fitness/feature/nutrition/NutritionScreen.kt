package com.jrussell.fitness.feature.nutrition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** Placeholder for the Nutrition screen until its milestone builds the real content. */
@Composable
fun NutritionScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.feature_nutrition_title),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
