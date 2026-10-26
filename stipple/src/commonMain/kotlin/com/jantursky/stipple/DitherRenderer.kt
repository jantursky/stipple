package com.jantursky.stipple

/** How a dither is drawn. Both renderers give the same picture; they differ in what they cost. */
public enum class DitherRenderer {
    /** One rectangle or dot per cell on the canvas. Works everywhere, including Android 6. The default. */
    Cells,

    /**
     * A single fragment shader for the whole element, when the device and the dither allow it: AGSL on Android 13
     * and later, a Skia runtime effect on desktop, iOS and web. Falls back to [Cells] when they don't.
     */
    Shader,

    /** The shader renderer for large surfaces, where the cell count gets high, and [Cells] for everything else. */
    Auto,
}

/**
 * A [DitherField] that also carries its shader, so the shader renderer can draw it. Fields without one are always
 * drawn by the cell renderer.
 */
public interface ShaderField : DitherField {
    /**
     * The field as a function in the shading language that AGSL and Skia's SkSL share, with exactly this signature:
     *
     * ```
     * float tone(float2 uv, float t, float aspect) { ... }
     * ```
     *
     * `uv` runs from 0 to 1 over the element, and `t` and `aspect` mean what they do in [DitherField.tone]. Keep the
     * result close to what [tone] returns, or the two renderers will disagree. The source must not change between
     * calls: it is compiled once and cached by its text.
     */
    public val shaderSource: String
}
