package com.jrussell.fitness.navigation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class TopLevelBackStackTest {
    @Test
    fun starts_onStartKey() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        assertThat(stack.backStack.toList()).containsExactly("today")
        assertThat(stack.topLevelKey).isEqualTo("today")
    }

    @Test
    fun switchTo_otherTab_stacksAfterStartTab() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        stack.switchTo("body")
        assertThat(stack.backStack.toList()).containsExactly("today", "body")
        assertThat(stack.topLevelKey).isEqualTo("body")
    }

    @Test
    fun switchTo_preservesEachTabsHistory() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        stack.switchTo("body")
        stack.push("detail")
        stack.switchTo("today")
        stack.switchTo("body")
        assertThat(stack.backStack.toList()).containsExactly("today", "body", "detail")
    }

    @Test
    fun removeLast_popsWithinTab() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        stack.switchTo("body")
        stack.push("detail")
        assertThat(stack.removeLast()).isTrue()
        assertThat(stack.backStack.toList()).containsExactly("today", "body")
    }

    @Test
    fun removeLast_atRootOfOtherTab_returnsToStartTab() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        stack.switchTo("body")
        assertThat(stack.removeLast()).isTrue()
        assertThat(stack.topLevelKey).isEqualTo("today")
        assertThat(stack.backStack.toList()).containsExactly("today")
    }

    @Test
    fun removeLast_atStartRoot_returnsFalse() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        assertThat(stack.removeLast()).isFalse()
        assertThat(stack.backStack.toList()).containsExactly("today")
    }

    @Test
    fun switchTo_currentTab_isNoOp() {
        val stack: TopLevelBackStack<String> = TopLevelBackStack("today")
        stack.push("detail")
        stack.switchTo("today")
        assertThat(stack.backStack.toList()).containsExactly("today", "detail")
    }
}
