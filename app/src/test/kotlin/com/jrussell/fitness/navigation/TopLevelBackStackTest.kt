package com.jrussell.fitness.navigation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.Test

class TopLevelBackStackTest {
    @Test
    fun starts_onStartKey() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        assertThat(stack.backStack.toList()).containsExactly("today")
        assertThat(stack.topLevelKey).isEqualTo("today")
    }

    @Test
    fun switchTo_otherTab_stacksAfterStartTab() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        assertThat(stack.backStack.toList()).containsExactly("today", "body")
        assertThat(stack.topLevelKey).isEqualTo("body")
    }

    @Test
    fun switchTo_preservesEachTabsHistory() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        stack.push("detail")
        stack.switchTo("today")
        stack.switchTo("body")
        assertThat(stack.backStack.toList()).containsExactly("today", "body", "detail")
    }

    @Test
    fun removeLast_popsWithinTab() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        stack.push("detail")
        assertThat(stack.removeLast()).isTrue()
        assertThat(stack.backStack.toList()).containsExactly("today", "body")
    }

    @Test
    fun removeLast_atRootOfOtherTab_returnsToStartTab() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        assertThat(stack.removeLast()).isTrue()
        assertThat(stack.topLevelKey).isEqualTo("today")
        assertThat(stack.backStack.toList()).containsExactly("today")
    }

    @Test
    fun removeLast_atStartRoot_returnsFalse() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        assertThat(stack.removeLast()).isFalse()
        assertThat(stack.backStack.toList()).containsExactly("today")
    }

    @Test
    fun switchTo_currentTab_isNoOp() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.push("detail")
        stack.switchTo("today")
        assertThat(stack.backStack.toList()).containsExactly("today", "detail")
    }

    @Test
    fun openOverlay_appendsOnTop() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        stack.openOverlay("settings")
        assertThat(stack.backStack.toList()).containsExactly("today", "body", "settings")
        assertThat(stack.overlay).isEqualTo("settings")
    }

    @Test
    fun openOverlay_twice_doesNotDuplicate() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.openOverlay("settings")
        stack.openOverlay("settings")
        assertThat(stack.backStack.toList()).containsExactly("today", "settings")
    }

    @Test
    fun switchTo_closesOverlay() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        stack.openOverlay("settings")
        stack.switchTo("nutrition")
        assertThat(stack.backStack.toList()).containsExactly("today", "nutrition")
        assertThat(stack.overlay).isNull()
    }

    @Test
    fun removeLast_closesOverlayFirst() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today", tabs = TABS)
        stack.switchTo("body")
        stack.push("detail")
        stack.openOverlay("settings")
        assertThat(stack.removeLast()).isTrue()
        assertThat(stack.overlay).isNull()
        assertThat(stack.backStack.toList()).containsExactly("today", "body", "detail")
    }

    @Test
    fun push_closesOverlay() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("home")
        stack.openOverlay("settings")

        stack.push("detail")

        assertThat(stack.overlay).isNull()
        assertThat(stack.backStack.last()).isEqualTo("detail")
    }

    private companion object {
        val TABS: List<String> = listOf("today", "nutrition", "body")
    }
}
