package com.jantursky.stipple.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect

/** Counts how often the composable that owns a component composes. */
class Counter {
    var count = 0
        private set

    // Not skippable, so it runs whenever the caller recomposes.
    @NonSkippableComposable
    @Composable
    fun Count() {
        SideEffect { count++ }
    }
}
