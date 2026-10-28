---
name: stipple
description: Build dithered UI in Compose Multiplatform with Stipple (io.github.jantursky:stipple). Use it when a user wants a dithered background, button, chart, gauge, progress bar, text or image in Compose, or wants to write a new dither field, pattern or component on Android, iOS, desktop or web.
---

# Stipple

Stipple draws dithers in Compose Multiplatform. A dither is made of three independent parts. Keep them separate and
new work stays small.

| Part | Type | Job |
| --- | --- | --- |
| Field | `DitherField` | Says how much ink each point wants: `tone(x, y, t, aspect)` returns 0 (paper) to 1 (ink). |
| Pattern | `DitherPattern` | A threshold map. A cell is ink when the tone is above its threshold. `DitherPatterns.Bayer2/4/8`, `BlueNoise`. |
| Theme | `StippleTheme` | `ink`, `paper`, optional `palette`, `cell` size in dp, `shape` (Square or Dot). |

Artifacts: `io.github.jantursky:stipple` (engine and modifiers) and `io.github.jantursky:stipple-components`
(`DitherButton`, `DitherBarChart`, `DitherGrid`, `DitherGauge`, `DitherStatCard`, `DitherProgressBar`, `DitherText`,
`DitherEffectText`, `DitherAvatar`, `DitherImage`, `DitherBackground`). Package `com.jantursky.stipple`, components in
`com.jantursky.stipple.components`. Targets: Android (minSdk 23), iOS, desktop, wasmJs.

## Choose the smallest tool

1. A ready component fits: use it. Every component takes `theme`, `pattern` and `animated` (false by default).
2. Any element should get a dither behind it: `Modifier.dither(field, pattern, theme)`.
3. A fill driven by a value: `Modifier.ditherProgress({ value })`.
4. Dither what an element already draws: `Modifier.ditherImage()` (photos, ordered or error diffusion),
   `Modifier.ditherFill(field)` (keeps the shape, swaps the colours), `Modifier.ditherMask(field)` (shows the content
   where the dither is lit; pair it with `DitherFields.dissolve({ progress })` or `sweep({ progress })` for a reveal).
5. A soft surface with several highlights: `DitherFields.mesh(tones, drift)`, a grid of 2 to 8 by 2 to 8 tones (Example 4).
6. Text that scrambles or dissolves in: `DitherEffectText(text, DitherTextEffect.Scramble)`, or pass a `progress`.
7. A look that doesn't exist yet: write a `DitherField`. Do not write a new renderer or a new component first.

## Rules

- **Draw phase only.** Animation clocks and progress values are read while drawing. Pass `progress` as a lambda
  (`ditherProgress({ state.value })`) and never read animated state in composition: that recomposes every frame.
- **Static by default.** Set `animated = true` only where motion carries meaning. Under reduced motion animations
  freeze on every platform; progress and value changes stay live because they are information. Use `stillTime` to pick
  the frame a frozen animated field shows.
- **Fields are pure.** No state, no allocation per call, no platform code. Multiply `x` by `aspect` before measuring a
  distance, or circles turn into ellipses. Return smooth tones and let the pattern make the dots.
- **Cell size is the cost.** The cell renderer draws about one rectangle per cell run. Above roughly 20,000 cells
  (a large background at 1-2 dp) pass `renderer = DitherRenderer.Auto`, or `Shader`. The shader renderer only draws
  `ShaderField`s (all the built-in fields) with any pattern up to 256 cells wide, square or dot cells and a theme of up to eight colours, behind the
  content or as `ditherFill` and `ditherMask`. Anything else, and Android below 13, uses cells automatically.
- **Contrast.** A dither shows less than full ink, so keep tone below about 0.5 where text sits. `DitherText` swaps the
  ink for black or white when `minContrast` (4.5) isn't met. Check `theme.contrastRatio` for custom colours.
- **Semantics.** Components expose normal semantics (button role, progress range, text). A plain `Modifier.dither` is
  decoration: give the element its own `contentDescription` when it carries meaning.
- **Theme once.** Wrap the app in `ProvideStippleTheme(theme)` and leave `theme = null` at call sites.
- Don't hardcode `Color`s inside fields. Fields return tones; colour belongs to the theme.

## Example 1: a custom animated field as a background

```kotlin
private val spiral = DitherField { x, y, t, aspect ->
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
```

`stillTime = 1f` is the frame shown when animation is off or reduced. Pick one that looks finished.

## Example 2: a dashboard tile from components

```kotlin
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
```

Pass `progress = null` for an indeterminate bar. No theme argument is needed: the components read the provided theme.

## Example 3: dither a photo and fade the headline in

```kotlin
@Composable
fun Cover(photo: Painter) {
    Box {
        Image(
            painter = photo,
            contentDescription = "Mountains at dusk",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .ditherImage(DitherPatterns.Bayer4, algorithm = DitherAlgorithm.Atkinson, key = "cover"),
        )
        DitherText("Dusk", Modifier.align(Alignment.BottomStart).padding(16.dp), animated = true)
    }
}
```

`ditherImage` reads the content back once, dithers it off the main thread and caches the result. Pass a `key` that
changes when the picture does. Atkinson and Floyd-Steinberg diffuse error and look better on photos; `Ordered` is
cheaper and steadier when the content changes.

## Example 4: a mesh gradient surface

```kotlin
@Composable
fun SoftSurface() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .dither(
                DitherFields.mesh(
                    tones = listOf(
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
```

`tones` has one list per row, top to bottom, and 2 to 8 rows and columns. Editing or animating the tones with the shader
renderer compiles nothing new.

## Example 5: reveal content with a dissolve

```kotlin
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
```

`reveal` goes from 0 (nothing shown) to 1 (everything shown) and is read while drawing, so moving it redraws the element
without recomposing it. `DitherFields.sweep({ reveal() })` reveals from left to right instead.

## Example 6: text that scrambles in

```kotlin
@Composable
fun Headline() {
    DitherEffectText("Stipple", DitherTextEffect.Scramble, charset = "01")
}
```

The text measures as its final value, shows at once under reduced motion, and a screen reader reads only the final
text. Pass `progress = { value }` to drive it yourself, or `DitherTextEffect.Dissolve` for grain-by-grain appearance.

## Palettes

Set `StippleTheme.palette` to a list of two to eight colours ordered from paper to ink (`Light.copy(palette = ...)`).
The tone picks a position on the ramp and the pattern decides between the two colours around it. Longer lists fall back
to the cell renderer.

## Writing a new dither: checklist

1. Write the field as a `DitherField` in common code and look at it with `Modifier.dither(field)`.
2. Check tones: the extremes must reach 0 and 1, and nothing outside 0..1 matters (values are clamped).
3. Try Bayer2, Bayer4, Bayer8 and BlueNoise, and a cell size of 2 to 6 dp, in the light and dark theme.
4. If it will cover a large area, give it a shader: implement `ShaderField` with a `shaderSource` that defines
   `float tone(float2 uv, float t, float aspect)` in the shading language AGSL and SkSL share, and test that it matches
   the Kotlin version.
5. Only then wrap it in a component, with `animated = false` as the default.
