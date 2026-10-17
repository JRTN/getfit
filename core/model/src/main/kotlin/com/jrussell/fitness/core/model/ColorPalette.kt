package com.jrussell.fitness.core.model

/** The user's color palette preference. Each palette has a light and a dark instance. */
enum class ColorPalette {
    /** Neutral grays with the violet accent. */
    DEFAULT,

    /** Okabe–Ito data colors, distinguishable with protanopia, deuteranopia and tritanopia. */
    COLOR_VISION_SAFE,

    /** Maximum luminance separation for low vision. */
    HIGH_CONTRAST,
}
