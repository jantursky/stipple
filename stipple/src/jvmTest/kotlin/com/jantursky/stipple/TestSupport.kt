package com.jantursky.stipple

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** Runs [block] in a 200 x 200 test window with a paused clock, so tests step frames themselves. */
@OptIn(ExperimentalTestApi::class)
fun runDitherTest(block: ComposeUiTest.() -> Unit) =
    runDesktopComposeUiTest(200, 200) {
        mainClock.autoAdvance = false
        block()
    }

/** Sets [content] at density 1, with full motion and the light theme unless told otherwise. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setDitherContent(
    motion: StippleMotion = StippleMotion.Full,
    theme: StippleTheme = StippleTheme.Light,
    content: @Composable () -> Unit,
) {
    setContent {
        CompositionLocalProvider(
            LocalDensity provides Density(1f),
            LocalStippleMotion provides motion,
            LocalStippleTheme provides theme,
        ) { content() }
    }
}

/** A 100 x 100 box carrying [modifier]. */
@Composable
fun Square(modifier: Modifier) {
    Box(modifier.size(100.dp))
}

/** The whole test window as pixels. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.capture(): ImageBitmap = onRoot().captureToImage()

/** The share of pixels in the top-left 100 x 100 square that are [color]. */
fun ImageBitmap.shareOf(color: Color): Float {
    val map = toPixelMap()
    var n = 0
    for (y in 0 until 100) for (x in 0 until 100) if (map[x, y] == color) n++
    return n / 10_000f
}

/** How many pixels differ between two images of the same size. */
fun ImageBitmap.pixelsDifferentFrom(other: ImageBitmap): Int {
    val a = toPixelMap()
    val b = other.toPixelMap()
    var n = 0
    for (y in 0 until height) for (x in 0 until width) if (a[x, y] != b[x, y]) n++
    return n
}

/** Counts how often the composable that owns a dither composes. */
class CompositionCounter {
    var count = 0
        private set

    // Not skippable, so it runs whenever the caller recomposes.
    @NonSkippableComposable
    @Composable
    fun Count() {
        SideEffect { count++ }
    }
}
