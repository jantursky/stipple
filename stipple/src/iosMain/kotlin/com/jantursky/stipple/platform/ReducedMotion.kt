package com.jantursky.stipple.platform

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

internal actual fun platformReducedMotion(): Boolean = UIAccessibilityIsReduceMotionEnabled()
