package com.jantursky.stipple

/**
 * Settings for dithering an image, next to the algorithm and the pattern. The neutral values change nothing, so
 * [Default] gives the same cells as dithering without options.
 *
 * Tone controls apply to every cell's brightness before the theme decides what is ink: first [contrast] around the
 * middle, then [brightness], then [gamma]. Instances compare by value, so passing an equal one doesn't rebuild a dither.
 *
 * @property brightness Added to every tone, from -1 to 1. 0 changes nothing, more makes more cells light.
 * @property contrast Scales tones around the middle, from 0 to 4. 1 changes nothing, more separates light from dark.
 * @property gamma Bends the mid-tones, from 0.1 to 10. 1 changes nothing, more brightens them, less darkens them.
 * @property threshold Shifts the point where a cell flips between two colors, from -0.5 to 0.5. 0 is the middle.
 * @property strength How much of the rounding error error-diffusion algorithms push on, from 0 to 1. 0 rounds every
 *   cell on its own. Ordered dithering ignores it.
 * @property serpentine Scan odd rows right to left and mirror the kernel, which hides the diagonal stripes plain
 *   left-to-right diffusion leaves in flat areas. Only error-diffusion algorithms use it.
 * @property linearLuminance Average brightness in linear light instead of on the encoded values. It is truer for mixed
 *   black-and-white cells and brightens mid-tones, so it is off by default.
 * @property sampling How many source pixels per cell side are read back, at least 1. Higher is sharper and slower;
 *   the default 2 is enough for cells that average anyway.
 */
public class ImageDitherOptions(
    brightness: Float = 0f,
    contrast: Float = 1f,
    gamma: Float = 1f,
    threshold: Float = 0f,
    strength: Float = 1f,
    public val serpentine: Boolean = false,
    public val linearLuminance: Boolean = false,
    sampling: Int = 2,
) {
    public val brightness: Float = brightness.coerceIn(-1f, 1f)
    public val contrast: Float = contrast.coerceIn(0f, 4f)
    public val gamma: Float = gamma.coerceIn(0.1f, 10f)
    public val threshold: Float = threshold.coerceIn(-0.5f, 0.5f)
    public val strength: Float = strength.coerceIn(0f, 1f)
    public val sampling: Int = sampling.coerceAtLeast(1)

    /** A copy with the given settings changed. */
    public fun copy(
        brightness: Float = this.brightness,
        contrast: Float = this.contrast,
        gamma: Float = this.gamma,
        threshold: Float = this.threshold,
        strength: Float = this.strength,
        serpentine: Boolean = this.serpentine,
        linearLuminance: Boolean = this.linearLuminance,
        sampling: Int = this.sampling,
    ): ImageDitherOptions =
        ImageDitherOptions(brightness, contrast, gamma, threshold, strength, serpentine, linearLuminance, sampling)

    override fun equals(other: Any?): Boolean =
        other is ImageDitherOptions &&
            brightness == other.brightness &&
            contrast == other.contrast &&
            gamma == other.gamma &&
            threshold == other.threshold &&
            strength == other.strength &&
            serpentine == other.serpentine &&
            linearLuminance == other.linearLuminance &&
            sampling == other.sampling

    override fun hashCode(): Int {
        var result = brightness.hashCode()
        result = 31 * result + contrast.hashCode()
        result = 31 * result + gamma.hashCode()
        result = 31 * result + threshold.hashCode()
        result = 31 * result + strength.hashCode()
        result = 31 * result + serpentine.hashCode()
        result = 31 * result + linearLuminance.hashCode()
        result = 31 * result + sampling
        return result
    }

    override fun toString(): String =
        "ImageDitherOptions(brightness=$brightness, contrast=$contrast, gamma=$gamma, threshold=$threshold, " +
            "strength=$strength, serpentine=$serpentine, linearLuminance=$linearLuminance, sampling=$sampling)"

    public companion object {
        /** The neutral options: every control at the value that changes nothing. */
        public val Default: ImageDitherOptions = ImageDitherOptions()
    }
}
