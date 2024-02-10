package com.bluedragonmc.jukebox.util

import kotlin.math.cos
import kotlin.math.sin

data class Vec2d(val x: Double, val y: Double) {
    fun rotate(radians: Double): Vec2d {
        val ca = cos(radians)
        val sa = sin(radians)
        return Vec2d(
            ca * x - sa * y,
            sa * x - ca * y
        )
    }
}