package com.jantursky.stipple.samples.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.stipple.samples.gallery.Catalog
import com.jantursky.stipple.samples.gallery.GalleryApp
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GalleryScreenshotTest {
    private fun shoot(
        page: String,
        dark: Boolean = false,
        width: Int = 1280,
        height: Int = 900,
        name: String = "$page${if (dark) "-dark" else ""}",
    ) = runDesktopComposeUiTest(width, height) {
        // The frame meter asks for a frame forever, so the window is never idle: step the clock instead.
        mainClock.autoAdvance = false
        setContent { GalleryApp(initialPage = page, initialDark = dark) }
        mainClock.advanceTimeBy(800)
        onRoot().captureRoboImage("src/jvmTest/screenshots/$name.png")
    }

    @Test fun overview() = shoot("overview")

    @Test fun overviewDark() = shoot("overview", dark = true)

    @Test fun fieldsDark() = shoot("fields", dark = true)

    @Test fun gaugeNarrow() = shoot("gauge", width = 390, height = 844, name = "gauge-narrow")

    @Test fun everyPage() = Catalog.forEach { shoot(it.id) }
}
