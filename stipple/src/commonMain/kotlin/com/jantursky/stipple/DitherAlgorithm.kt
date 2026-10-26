package com.jantursky.stipple

/** How [ditherImage] turns an image into cells. */
public enum class DitherAlgorithm {
    /** Compares each cell with the pattern. Fast, regular, and the same every time. */
    Ordered,

    /** Floyd-Steinberg error diffusion: pushes each cell's rounding error onto its neighbours. Smooth and organic. */
    FloydSteinberg,

    /** Atkinson error diffusion: spreads only part of the error, which keeps highlights and shadows clean. */
    Atkinson,
}
