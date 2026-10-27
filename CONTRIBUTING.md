# Contributing

Thanks for helping. Bug reports, docs fixes and pull requests are all welcome.

## Before you start

- For anything bigger than a small fix, open an issue first so we can agree on the approach.
- New fields and components need a clear use. Say what the piece is for and what it adds over a field written in a few lines.
- Visual bugs depend on the platform. Say which target, device and OS version you saw it on, and attach a screen recording if you can.

## Setup

You need JDK 17 and an Android SDK. The Gradle wrapper handles the rest. For iOS you need Xcode on macOS.

The docs site and the browser tests use [Bun](https://bun.sh) 1.3+ and Node.js 22.19+ (the version is in `.node-version`).

```sh
./gradlew :stipple:jvmTest                              # desktop tests, quickest feedback
./gradlew :samples:web:wasmJsBrowserDevelopmentRun    # the gallery in the browser
./gradlew :samples:desktop:run                        # the same gallery in a desktop window 
./gradlew :samples:androidApp:installDebug            # the gallery app on a running emulator or device
scripts/device-smoke.sh android                       # open every page on an emulator, screenshot it, report crashes
scripts/device-smoke.sh ios                           # the same on the iPhone 17 Pro simulator (needs xcodegen)
```

The iOS app is an Xcode project generated from `samples/iosApp/project.yml`: run `xcodegen` in `samples/iosApp`,
open `GalleryApp.xcodeproj` and run it on a simulator; its build phase builds the Kotlin framework with Gradle.

## Where things live

| path                                 | what                                                                                 |
| ------------------------------------ | ------------------------------------------------------------------------------------ |
| `stipple/src/commonMain`             | The engine: patterns, fields, theme, the modifier nodes and both renderers.          |
| `stipple/src/*Main/.../platform`     | Reduced-motion readers for Android, iOS, the browser and the JVM.                    |
| `stipple/src/skikoMain`, `androidMain` | The shader painters: Skia runtime effects, and AGSL on Android 13+.               |
| `stipple-components`                 | The ten components, built on the engine.                                             |
| `skills/stipple`                     | The agent skill, published with the docs. Its examples are compiled by a test.       |
| `samples/gallery`                    | The gallery's examples, their controls and the catalog, shared by every host. |
| `samples/web`                        | The gallery, compiled to Kotlin/Wasm. Each example's source is shown next to it. |
| `samples/desktop`                    | The same gallery in a desktop window (JVM).                                          |
| `samples/app`                        | The gallery app for phones and tablets; the iOS `GalleryKit` framework. |
| `samples/androidApp`                 | The Android app around `samples/app`.                                                |
| `samples/iosApp`                     | The iOS app around `GalleryKit`: a SwiftUI shell, generated with xcodegen.           |
| `apps/docs`                          | The docs site. Its `skills` folder entry publishes the agent skill.                    |
| `design/`                            | The design system the gallery and the docs use.                                      |

## Checks

```sh
./gradlew spotlessCheck apiCheck :stipple:jvmTest :stipple-components:jvmTest :stipple:testAndroidHostTest :stipple:koverVerify :stipple-components:koverVerify   # what CI runs
./gradlew :stipple:iosSimulatorArm64Test     # on macOS
./gradlew :stipple:verifyRoborazziJvm        # screenshot tests; recordRoborazziJvm after an intended visual change
./gradlew spotlessApply                    # format the code (ktlint_official)
./gradlew apiDump                          # after changing the public API
bun run build:docs                         # from the repository root
bun run test:web                           # browser tests of the gallery, after :samples:web:wasmJsBrowserDistribution
```

CI runs all of them on every pull request. `koverVerify` fails the build when coverage drops below the gate in the Gradle build.

### Docs code samples

Every ```` ```kotlin ```` fence in `apps/docs/docs` is compiled by `:stipple-components:jvmTest`, so a sample that no longer compiles fails the build. `:stipple-components:generateDocsSnippets` writes them to `stipple-components/build/generated/docsSnippets`, one file per page; a compile error points there, and the comment above each snippet names the docs file and line.

- A plain fence is the body of a composable, with the library's packages imported and the names `target`, `App()` and `Screen()` available.
- ```` ```kotlin top ```` is placed at file level, for declarations such as an `object`.
- ```` ```kotlin nocompile ```` is left out. Use it only for what cannot be code: Gradle configuration and signatures without bodies.

### Frame times on a device

```sh
scripts/frame-bench.sh android --seconds 10 --repeat 3   # an attached phone or a running emulator
IOS_DEVICE_UDID=<udid> scripts/frame-bench.sh ios        # a physical iPhone, with the signed app installed
```

It opens the gallery's `bench` page once per renderer (`--renderer cells|shader|both`) and writes `results.csv` (one row per run: platform, device, OS, refresh rate, renderer, frames, p50, p90, p95, p99 and the share of frames over one refresh interval) and `summary.md` (the median run per renderer, with the device, the commit and the run-to-run spread) to `build/frame-bench`. Android frames come from `dumpsys gfxinfo framestats`; the iPhone path uses Instruments' "Animation Hitches" template through `xctrace` and has not been verified on a device yet. Run `scripts/frame-bench.sh` without arguments for every option.

## Conventions

- Kotlin in the official style. Run `./gradlew spotlessApply` before you push.
- Everything that isn't public API is `internal`. Changing the public API means running `./gradlew apiDump` and reviewing the diff in each module's `api` folder.
- Animation frames must not recompose. Modifiers keep their clock in a `Modifier.Node` and read it in the draw phase; the recomposition tests in `stipple/src/jvmTest` check this.
- Fields, patterns and the quantizer are plain functions, tested in `commonTest`. A field that gets a shader keeps both versions in step: the renderer test compares their output.
- Mark anything you could not run on a platform as unverified in the docs, and say which devices you did check.
- Add a line under `## [Unreleased]` in `CHANGELOG.md` for anything users will notice.

## License

By contributing you agree that your contributions are licensed under the MIT license.
