package com.jantursky.stipple.platform

import android.animation.ValueAnimator
import android.os.Build

// Below API 26 there is no static check. Compose's MotionDurationScale, which follows the animator duration
// scale on every API level, covers those versions.
internal actual fun platformReducedMotion(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled()
