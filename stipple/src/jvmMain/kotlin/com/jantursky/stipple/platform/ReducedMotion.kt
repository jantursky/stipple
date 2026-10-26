package com.jantursky.stipple.platform

// Desktop systems don't share a reduced-motion setting Java can read. Apps can set LocalKapowMotion.
internal actual fun platformReducedMotion(): Boolean = false
