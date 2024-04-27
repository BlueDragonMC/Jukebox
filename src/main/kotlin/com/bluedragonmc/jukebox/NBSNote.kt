package com.bluedragonmc.jukebox

import com.bluedragonmc.jukebox.api.Note
import com.bluedragonmc.jukebox.util.byte
import dev.simplix.protocolize.data.Sound
import java.nio.ByteBuffer

class NBSNote(
    override val instrument: Byte,
    override val key: Byte,
    override val velocity: Byte?,
    override val pan: Byte?,
    override val pitch: Short?,
) : Note {
    companion object {
        fun read(version: NBSFormat, buffer: ByteBuffer): NBSNote = buffer.run {
            if (version >= NBSFormat.MODERN_4) {
                NBSNote(byte, byte, byte, byte, short)
            } else {
                NBSNote(byte, byte, null, null, null)
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
    }

    override fun getSound(): Sound {
        return if (instrument >= sounds.size) sounds.last() else sounds[instrument.toInt()]
    }
}