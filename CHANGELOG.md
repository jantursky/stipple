# Changelog

All notable changes to Stipple are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0]

### Added

- Dither engine: Bayer 2x2, 4x4 and 8x8 and blue-noise patterns, an extensible `DitherPattern`, pure `DitherField`s (solid, linear, radial, noise, waves, ripples, sweep), and `StippleTheme` through a composition local.
- Cell renderer for every target, and an optional shader renderer (AGSL on Android 13 and later, Skia runtime effects on desktop, iOS and web) with automatic fallback to cells.
- Modifiers `dither`, `ditherProgress`, `ditherImage` (ordered, Floyd-Steinberg and Atkinson), `ditherFill` and `ditherMask`.
- Reduced motion that freezes animation on Android, iOS, desktop and web.
- Components: `DitherButton`, `DitherBarChart`, `DitherGrid`, `DitherGauge`, `DitherStatCard`, `DitherProgressBar`, `DitherText`, `DitherAvatar`, `DitherImage` and `DitherBackground`.
- Published on JitPack as `com.github.jantursky.stipple:stipple` and `stipple-components`.
- Gallery apps for web, desktop, Android and iOS, and an agent skill (`skills/stipple/SKILL.md`).
