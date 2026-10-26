package com.jantursky.stipple.samples.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.samples.gallery.examples.Scene
import com.jantursky.stipple.samples.gallery.examples.ScenesExample
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** One sheet with every scene, for judging a change to a field at a glance. */
@OptIn(ExperimentalTestApi::class)
class ScenesSheetTest {
    @Test
    fun sheet() =
        runDesktopComposeUiTest(1300, 900) {
            mainClock.autoAdvance = false
            setContent {
                Column(Modifier.background(Color.White).padding(8.dp)) {
                    Scene.entries.chunked(3).forEach { row ->
                        Row {
                            row.forEach { scene ->
                                Box(Modifier.padding(4.dp).size(416.dp, 200.dp)) {
                                    ScenesExample(scene, StippleTheme.Light, DitherPatterns.Bayer4, animated = false)
                                }
                            }
                        }
                    }
                }
            }
            mainClock.advanceTimeBy(300)
            onRoot().captureRoboImage("src/jvmTest/screenshots/scenes-sheet.png")
        }
}
