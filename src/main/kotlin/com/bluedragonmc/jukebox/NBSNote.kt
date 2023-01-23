package com.bluedragonmc.jukebox

import com.velocitypowered.api.proxy.Player
import dev.simplix.protocolize.api.Location
import dev.simplix.protocolize.api.Protocolize
import dev.simplix.protocolize.api.SoundCategory
import dev.simplix.protocolize.data.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

class NBSNote(val instrument: Byte, val key: Byte, val velocity: Byte?, val pan: Byte?, val pitch: Short?) {
    companion object {
        fun read(version: NBSFormat, buffer: ByteBuffer): NBSNote = buffer.run {
            if (version >= NBSFormat.MODERN_4) {
                NBSNote(byte, byte, byte, byte, short)
            } else {
                NBSNote(byte, byte, null, null, null)
            }
        }
    }

    private fun getHorizontalDirection(location: Location): Vec2d {
        val rotX: Float = location.yaw()
        val rotY: Float = location.pitch()
        val xz = cos(Math.toRadians(rotY.toDouble()))
        return Vec2d(
            -xz * sin(Math.toRadians(rotX.toDouble())),
            xz * cos(Math.toRadians(rotX.toDouble()))
        )
    }

    fun playTo(player: Player) {
        val wrappedPlayer = Protocolize.playerProvider().player(player.uniqueId)
        if (wrappedPlayer == null) {
            Song.stop(player)
            return
        }
        val position = if (pan != null) {
            // 0 => 2 blocks to the right
            // 100 => center
            // 200 => 2 blocks to the left
            if (pan == 100.toByte()) wrappedPlayer.location()
            else {
                val adjusted =
                    -(pan - 100) * (90f / 100f) * (PI / 180.0) // Convert the pan value into a rotation amount in radians
                val direction = getHorizontalDirection(wrappedPlayer.location())
                val offset = direction.rotate(adjusted)
                Location(
                    wrappedPlayer.location().x() + offset.x,
                    wrappedPlayer.location().y(),
                    wrappedPlayer.location().z() + offset.y,
                    0f,
                    0f
                )
            }
        } else {
            wrappedPlayer.location()
        }

        try {
            wrappedPlayer.playSound(
                position,
                getSound(),
                SoundCategory.RECORDS,
                (velocity?.toFloat() ?: 100f) / 100f,
                2f.pow((key - 45) / 12f)
            )
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            player.sendMessage(
                Component.text(
                    "There was an error playing this song! Please contact a server administrator.",
                    NamedTextColor.RED
                )
            )
        }
    }

    private val sounds = arrayOf(
        Sound.BLOCK_NOTE_BLOCK_HARP,
        Sound.BLOCK_NOTE_BLOCK_BASS,
        Sound.BLOCK_NOTE_BLOCK_BASEDRUM,
        Sound.BLOCK_NOTE_BLOCK_SNARE,
        Sound.BLOCK_NOTE_BLOCK_HAT,
        Sound.BLOCK_NOTE_BLOCK_GUITAR,
        Sound.BLOCK_NOTE_BLOCK_FLUTE,
        Sound.BLOCK_NOTE_BLOCK_BELL,
        Sound.BLOCK_NOTE_BLOCK_CHIME,
        Sound.BLOCK_NOTE_BLOCK_XYLOPHONE,
        Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE,
        Sound.BLOCK_NOTE_BLOCK_COW_BELL,
        Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO,
        Sound.BLOCK_NOTE_BLOCK_BIT,
        Sound.BLOCK_NOTE_BLOCK_BANJO,
        Sound.BLOCK_NOTE_BLOCK_PLING
    )

    private fun getSound(): Sound {
        return if (instrument >= sounds.size) sounds.last() else sounds[instrument.toInt()]
    }
}