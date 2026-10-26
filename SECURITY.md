# Security policy

## Supported versions

Only the latest release of `com.github.jantursky.stipple:stipple` and its companion artifacts gets fixes.

## Reporting a vulnerability

Please don't open a public issue. Use GitHub's [private vulnerability reporting](https://github.com/jantursky/stipple/security/advisories/new), or email jan.tursky@gmail.com with a description and, if you can, a minimal reproduction.

You'll get an answer within a week. Once a fix is released, the advisory is published with credit, unless you'd rather stay anonymous.

## Things to know

- Stipple draws effects on the device. It makes no network calls, collects no data and needs no permissions.
- It reads the platform's reduced-motion setting: the animator duration scale on Android, Reduce Motion on iOS and `prefers-reduced-motion` in the browser.
- Haptics go through the `StippleFeedback` handler your app provides. The optional `stipple-multihaptic` artifact calls multihaptic, which uses the platform vibration APIs.
- Text and painters you pass as particles are drawn as given. Stipple doesn't parse or load anything.
