package com.jantursky.stipple.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.ProvideStippleTheme
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.ditherImage
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test

/**
 * The three examples in skills/stipple/SKILL.md, verbatim. They only have to compile, so the skill never teaches an
 * API that has moved.
 */
class SkillExamplesCompileTest {
    @Test
    fun theExamplesCompile() {
        // Referencing the functions keeps them from being removed as unused.
        listOf<Any>(::SpiralHero, ::UploadTile, ::Cover)
    }
}

private val spiral =
    DitherField { x, y, t, aspect ->
        val dx = (x - 0.5f) * aspect
        val dy = y - 0.5f
        0.5f + 0.5f * sin(hypot(dx, dy) * 14f - atan2(dy, dx) * 2f - t * 2f)
    }

@Composable
fun SpiralHero() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp)
            .dither(spiral, DitherPatterns.Bayer8, animated = true, stillTime = 1f),
    )
}

@Composable
fun UploadTile(progress: Float) {
    ProvideStippleTheme(StippleTheme.Light.copy(cell = 2.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DitherStatCard(
                label = "Uploads",
                value = "1,284",
                delta = "+12%",
                trend = listOf(3f, 5f, 4f, 7f, 6f, 9f),
            )
            DitherProgressBar(progress = progress, contentDescription = "Upload progress")
            DitherButton(text = "Retry", onClick = { })
        }
    }
}

@Composable
fun Cover(photo: Painter) {
    Box {
        Image(
            painter = photo,
            contentDescription = "Mountains at dusk",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .ditherImage(DitherPatterns.Bayer4, algorithm = DitherAlgorithm.Atkinson, key = "cover"),
        )
        DitherText("Dusk", Modifier.align(Alignment.BottomStart).padding(16.dp), animated = true)
    }
}
