package com.jantursky.stipple.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.jantursky.stipple.DitherAlgorithm
import com.jantursky.stipple.DitherField
import com.jantursky.stipple.DitherFields
import com.jantursky.stipple.DitherPatterns
import com.jantursky.stipple.ProvideStippleTheme
import com.jantursky.stipple.StippleTheme
import com.jantursky.stipple.dither
import com.jantursky.stipple.ditherImage
import com.jantursky.stipple.ditherMask
import java.io.File
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The six examples in skills/stipple/SKILL.md, verbatim. They only have to compile, so the skill never teaches an
 * API that has moved.
 */
class SkillExamplesCompileTest {
    @Test
    fun theExamplesCompile() {
        // Referencing the functions keeps them from being removed as unused.
        listOf<Any>(::SpiralHero, ::UploadTile, ::Cover, ::SoftSurface, ::Reveal, ::Headline)
    }

    @Test
    fun everySkillExampleIsCompiledHere() {
        val skill = File("../skills/stipple/SKILL.md").readText()
        val source = File("src/jvmTest/kotlin/com/jantursky/stipple/components/SkillExamplesCompileTest.kt").readText()
        val fences =
            Regex("```kotlin\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
                .findAll(skill)
                .map {
                    it.groupValues[1]
                }.toList()
        assertTrue(
            fences.size >= 6,
            "found only ${fences.size} Kotlin fences in ${File("../skills/stipple/SKILL.md").absolutePath}",
        )
        val squashed = source.squashed()
        fences.forEachIndexed { index, fence ->
            assertTrue(
                fence.squashed() in squashed,
                "skill example ${index + 1} differs from the copy in SkillExamplesCompileTest",
            )
        }
    }

    private fun String.squashed() =
        replace(Regex("\\s+"), " ")
            .replace("( ", "(")
            .replace(" )", ")")
            .replace(",)", ")")
            .trim()
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

@Composable
fun SoftSurface() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .dither(
                DitherFields.mesh(
                    tones =
                        listOf(
                            listOf(0.9f, 0.4f, 0.2f),
                            listOf(0.3f, 0.7f, 0.3f),
                            listOf(0.1f, 0.3f, 0.8f),
                        ),
                    drift = 0.3f,
                ),
                DitherPatterns.Bayer8,
                animated = true,
                stillTime = 1f,
            ),
    )
}

@Composable
fun Reveal(reveal: () -> Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(Color(0xFF615FFF))
            .ditherMask(DitherFields.dissolve(reveal), DitherPatterns.Bayer4),
    )
}

@Composable
fun Headline() {
    DitherEffectText("Stipple", DitherTextEffect.Scramble, charset = "01")
}
