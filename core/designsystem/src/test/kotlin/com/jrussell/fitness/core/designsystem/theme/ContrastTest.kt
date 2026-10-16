package com.jrussell.fitness.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import assertk.assertThat
import assertk.assertions.isCloseTo
import org.junit.Test

class ContrastTest {
    @Test
    fun blackOnWhite_is21() {
        assertThat(contrastRatio(Color.Black, Color.White)).isCloseTo(21.0, 0.01)
    }

    @Test
    fun sameColour_is1() {
        assertThat(contrastRatio(Color(0xFF6941C6), Color(0xFF6941C6))).isCloseTo(1.0, 0.01)
    }

    @Test
    fun isSymmetric() {
        val forward: Double = contrastRatio(Color(0xFF777777), Color.White)
        val backward: Double = contrastRatio(Color.White, Color(0xFF777777))
        assertThat(forward).isCloseTo(backward, 0.0001)
    }

    @Test
    fun grey777OnWhite_is4_48() {
        assertThat(contrastRatio(Color(0xFF777777), Color.White)).isCloseTo(4.48, 0.01)
    }

    @Test
    fun violetOnDefaultLightBackground_is6_18() {
        assertThat(contrastRatio(Color(0xFF6941C6), Color(0xFFF7F7F8))).isCloseTo(6.18, 0.01)
    }
}
