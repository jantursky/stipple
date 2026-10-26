package com.jantursky.stipple.platform

private fun prefersReducedMotion(): Boolean =
    js("typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches")

internal actual fun platformReducedMotion(): Boolean = prefersReducedMotion()
