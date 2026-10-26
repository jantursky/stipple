package com.jantursky.stipple

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import com.jantursky.stipple.platform.platformReducedMotion
import kotlin.coroutines.CoroutineContext

/** How much motion animated dithers may use. */
public enum class StippleMotion {
    /** Follow the platform's reduced-motion setting. */
    System,

    /** Always animate. */
    Full,

    /** Never animate: every dither shows its still frame. */
    Reduced,
}

/** Overrides the reduced-motion setting for a part of the composition tree. Defaults to [StippleMotion.System]. */
public val LocalStippleMotion: ProvidableCompositionLocal<StippleMotion> = compositionLocalOf { StippleMotion.System }

/**
 * Whether dithers here are held at their still frame: [LocalStippleMotion] when it is [StippleMotion.Full] or
 * [StippleMotion.Reduced], otherwise the platform setting. Components use it to skip animations of their own.
 */
@Composable
public fun isStippleMotionReduced(): Boolean =
    resolveReducedMotion(LocalStippleMotion.current, rememberCoroutineScope().coroutineContext.durationScale)

/**
 * Compose sets [MotionDurationScale] to 0 when animations are off on Android and when Reduce Motion is on in iOS, so
 * that covers Android versions the platform check can't see.
 */
internal fun resolveReducedMotion(
    motion: StippleMotion,
    durationScale: Float,
): Boolean =
    when (motion) {
        StippleMotion.Full -> false
        StippleMotion.Reduced -> true
        StippleMotion.System -> durationScale == 0f || platformReducedMotion()
    }

internal val CoroutineContext.durationScale: Float
    get() = this[MotionDurationScale]?.scaleFactor ?: 1f
