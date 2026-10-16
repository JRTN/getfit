package com.jrussell.fitness.core.model

/** The user's colour palette preference. Each palette has a light and a dark instance. */
enum class ColorPalette {
    /** Neutral greys with the violet accent. */
    DEFAULT,

    /** Okabe–Ito data colours, distinguishable with protanopia, deuteranopia and tritanopia. */
    COLOUR_VISION_SAFE,

    /** Maximum luminance separation for low vision. */
    HIGH_CONTRAST,
}
