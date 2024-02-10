package com.bluedragonmc.jukebox.api

import com.bluedragonmc.jukebox.util.Vec2d
import dev.simplix.protocolize.data.Sound
import kotlin.math.cos
import kotlin.math.sin

interface Note {
    val instrument: Byte
    val key: Byte
    val velocity: Byte?
    val pan: Byte?
    val pitch: Short?

    fun getSound(): Sound

    fun getHorizontalDirection(yaw: Float, pitch: Float): Vec2d {
        val xz = cos(Math.toRadians(pitch.toDouble()))
        return Vec2d(
            -xz * sin(Math.toRadians(yaw.toDouble())),
            xz * cos(Math.toRadians(yaw.toDouble()))
        )
    }

}