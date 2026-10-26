package com.jantursky.stipple.platform

/** Whether the platform asks apps to reduce motion. Read when an effect fires, so no observer is needed. */
internal expect fun platformReducedMotion(): Boolean
