## What and why

<!-- What this changes, and the problem it solves. Link the issue if there is one. -->

## Checklist

- [ ] `./gradlew spotlessCheck apiCheck :stipple:jvmTest :stipple:testAndroidHostTest :stipple:koverVerify` passes
- [ ] `./gradlew :stipple:iosSimulatorArm64Test` passes, if iOS code changed
- [ ] `./gradlew apiDump` was run, if the public API changed
- [ ] New behavior has tests
- [ ] `./gradlew :stipple:verifyRoborazziJvm` passes, or screenshots were re-recorded on purpose
- [ ] The gallery in `samples/gallery` has an example, if this adds a field, modifier or component
- [ ] Docs in `apps/docs` are updated, if the behavior users see changed
- [ ] `CHANGELOG.md` has an entry under `## [Unreleased]`
