# Changelog

All notable changes to Stipple are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- The shader renderer draws `ditherFill` and `ditherMask`, and `DitherFields.noise` and `DitherFields.sweep` carry a shader. `ditherFill`, `ditherMask`, `DitherText` and `DitherImage` take a `renderer`, `DitherRenderer.Cells` by default.
- `DitherFields.sweep` accepts a `() -> Float` progress that is read when the field is drawn, and its shader takes the progress as a uniform, so an animated reveal compiles one program.

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
