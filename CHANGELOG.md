# Changelog

All notable changes to Stipple are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.1.1] - 2026-10-28

### Changed

- The agent skill (`skills/stipple/SKILL.md`) has worked examples for a mesh surface, a dissolve reveal and `DitherEffectText`, and a palette section. All six examples are compiled by a test, and a second test fails if the skill and the compiled copy differ.
- The docs site publishes the same skill the repository holds, and documents `charset`, `DEFAULT_SCRAMBLE_CHARSET` and `DitherTextEffect`. CI checks the links of the built docs.
- The gallery shows `DitherFields.dissolve` on the Fields and `ditherMask` pages.

## [1.1.0] - 2026-10-27

### Added

- The shader renderer draws `ditherFill` and `ditherMask`, and `DitherFields.noise` and `DitherFields.sweep` carry a shader. `ditherFill`, `ditherMask`, `DitherText` and `DitherImage` take a `renderer`, `DitherRenderer.Cells` by default.
- `DitherFields.sweep` accepts a `() -> Float` progress that is read when the field is drawn, and its shader takes the progress as a uniform, so an animated reveal compiles one program.
- `DitherFields.mesh`: a grid of 2 to 8 by 2 to 8 tones blended smoothly, with an optional drift of the interior points. It has a shader that takes the tones as a uniform, so editing or animating a mesh compiles nothing.
- `DitherEffectText` with `DitherTextEffect.Scramble` and `Dissolve`: text that settles out of random characters or appears grain by grain, from a timeline or a progress you own. It measures as the final text, shows it at once under reduced motion, and a screen reader reads only the final text.
- `DitherFields.dissolve`: a reveal in an irregular order, for `ditherMask`, with a shader that takes the progress as a uniform.
- The shader renderer draws blue noise and custom patterns up to 256 cells wide, dot-shaped cells and palettes of up to eight colours. Larger patterns and longer palettes still use the cell renderer.

### Changed

- The noise field uses a float-only hash so the cell renderer and the shader draw the same cells. Which cells light up for a given noise changes; the look does not.

## [1.0.0]

### Added

- Dither engine: Bayer 2x2, 4x4 and 8x8 and blue-noise patterns, an extensible `DitherPattern`, pure `DitherField`s (solid, linear, radial, noise, waves, ripples, sweep), and `StippleTheme` through a composition local.
- Cell renderer for every target, and an optional shader renderer (AGSL on Android 13 and later, Skia runtime effects on desktop, iOS and web) with automatic fallback to cells.
- Modifiers `dither`, `ditherProgress`, `ditherImage` (ordered, Floyd-Steinberg and Atkinson), `ditherFill` and `ditherMask`.
- Reduced motion that freezes animation on Android, iOS, desktop and web.
- Components: `DitherButton`, `DitherBarChart`, `DitherGrid`, `DitherGauge`, `DitherStatCard`, `DitherProgressBar`, `DitherText`, `DitherAvatar`, `DitherImage` and `DitherBackground`.
- Gallery apps for web, desktop, Android and iOS, and an agent skill (`skills/stipple/SKILL.md`).
