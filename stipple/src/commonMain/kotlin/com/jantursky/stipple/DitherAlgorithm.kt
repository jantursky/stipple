package com.jantursky.stipple

/** How [ditherImage] turns an image into cells. */
public enum class DitherAlgorithm {
    /** Compares each cell with the pattern. Fast, regular, and the same every time. */
    Ordered,

    /** Floyd-Steinberg error diffusion: pushes each cell's rounding error onto its neighbours. Smooth and organic. */
    FloydSteinberg,

    /** Atkinson error diffusion: spreads only part of the error, which keeps highlights and shadows clean. */
    Atkinson,

    /** Sierra (three rows) error diffusion: a wide, smooth kernel between Floyd-Steinberg and Stucki. */
    Sierra,

    /** Sierra Lite error diffusion: three neighbours only, fast and slightly coarser. */
    SierraLite,

    /** Burkes error diffusion: two rows, wide and cheap, with fine and even grain. */
    Burkes,

    /** Stucki error diffusion: a wide kernel that gives sharp, clean edges. */
    Stucki,

    /** Jarvis-Judice-Ninke error diffusion: the widest kernel, very smooth gradients and soft grain. */
    JarvisJudiceNinke,
}
