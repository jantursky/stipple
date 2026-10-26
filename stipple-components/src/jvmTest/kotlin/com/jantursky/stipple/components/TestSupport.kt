package com.jantursky.stipple.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.jantursky.stipple.LocalStippleMotion
import com.jantursky.stipple.LocalStippleTheme
import com.jantursky.stipple.StippleMotion
import com.jantursky.stipple.StippleTheme

/** Runs [block] in a 300 x 300 window with a paused clock, at density 1. */
@OptIn(ExperimentalTestApi::class)
fun runComponentTest(block: ComposeUiTest.() -> Unit) =
    runDesktopComposeUiTest(300, 300) {
        mainClock.autoAdvance = false
        block()
    }

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setComponent(
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

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.capture(): ImageBitmap = onRoot().captureToImage()

/** The share of pixels in a rectangle of the image that are [color]. */
fun ImageBitmap.shareOf(
    color: Color,
    x: Int = 0,
    y: Int = 0,
    width: Int = this.width,
    height: Int = this.height,
): Float {
    val map = toPixelMap()
    var n = 0
    for (j in y until y + height) for (i in x until x + width) if (map[i, j] == color) n++
    return n / (width * height).toFloat()
}

fun ImageBitmap.pixelsDifferentFrom(other: ImageBitmap): Int {
    val a = toPixelMap()
    val b = other.toPixelMap()
    var n = 0
    for (y in 0 until height) for (x in 0 until width) if (a[x, y] != b[x, y]) n++
    return n
}
