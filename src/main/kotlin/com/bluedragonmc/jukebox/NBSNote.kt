package com.bluedragonmc.jukebox

import com.bluedragonmc.jukebox.api.Note
import com.bluedragonmc.jukebox.util.byte
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
    }
}