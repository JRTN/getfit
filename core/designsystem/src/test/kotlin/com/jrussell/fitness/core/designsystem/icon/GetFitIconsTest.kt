package com.jrussell.fitness.core.designsystem.icon

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetFitIconsTest {
    @Test
    fun everyIcon_inflatesAsVectorDrawable() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val expectedSizePx: Int = (ICON_SIZE_DP * context.resources.displayMetrics.density).toInt()
        GetFitIcons.all.forEach { (name: String, id: Int) ->
            val drawable: Drawable? = ContextCompat.getDrawable(context, id)
            assertThat(drawable, name).isNotNull().isInstanceOf(VectorDrawable::class)
            assertThat(drawable?.intrinsicWidth, name).isEqualTo(expectedSizePx)
            assertThat(drawable?.intrinsicHeight, name).isEqualTo(expectedSizePx)
        }
    }

    @Test
    fun directionalIcons_mirrorInRightToLeftLayouts() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val directional: List<Pair<String, Int>> = listOf(
            "Back" to GetFitIcons.Back,
        )
        directional.forEach { (name: String, id: Int) ->
            assertThat(ContextCompat.getDrawable(context, id)?.isAutoMirrored, name).isEqualTo(true)
        }
    }

    private companion object {
        const val ICON_SIZE_DP: Int = 24
    }
}
