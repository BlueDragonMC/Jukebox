package com.bluedragonmc.jukebox.impl

import com.bluedragonmc.jukebox.NBSFormat
import com.bluedragonmc.jukebox.NBSNote
import com.bluedragonmc.jukebox.api.Song
import com.bluedragonmc.jukebox.api.SongLoader
import com.bluedragonmc.jukebox.util.byte
import com.bluedragonmc.jukebox.util.string
import com.bluedragonmc.jukebox.util.unsignedShort
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NBSSongLoader : SongLoader {
    override fun load(source: String?, bytes: ByteArray): Song {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val firstBytes =
            buffer.unsignedShort // The first two bytes could be 00 (new NBS format) or the length of the song (NBS classic format)
        val songLength: Short
        val version: NBSFormat
        if (firstBytes == 0) {
            // New OpenNBS format
            val versionByte = buffer.byte // the new OpenNBS format version (0-5)
            version = NBSFormat.modern(versionByte)
            buffer.byte // the index at which custom instruments start (vanilla instrument count)
            if (version >= NBSFormat.MODERN_3) {
                songLength = buffer.short // song length in ticks
            } else {
                songLength = -1 // song length must be calculated based on the song's layers
            }
        } else {
            version = NBSFormat.CLASSIC
            songLength = firstBytes.toShort()
        }
        val layerCount = buffer.unsignedShort
        val songName = buffer.string.ifEmpty { "Unknown" }
        val author = buffer.string.ifEmpty { "Unknown" }
        val originalAuthor = buffer.string
        val description = buffer.string
        val tempo = buffer.unsignedShort / 100.0
        buffer.byte // auto-saving enabled
        buffer.byte // auto-saving duration
        buffer.byte // time signature
        buffer.int // minutes spent editing
        buffer.int // total left clicks
        buffer.int // total right clicks
        buffer.int // total note blocks added
        buffer.int // total note blocks removed
        buffer.string // .midi file name
        if (version >= NBSFormat.MODERN_4) {
            buffer.byte // loop
            buffer.byte // max loop count
            buffer.unsignedShort // loop start tick
        }

        // Read the song's notes
        var tick = -1
        val ticks = arrayOfNulls<Collection<NBSNote>>(songLength + 1)

        while (true) {
            val tickJumps = buffer.unsignedShort // the amount of "jumps" to the next tick with a note (horizontal)
            if (tickJumps == 0) break
            tick += tickJumps

            // Read the layers at this tick
            val notes = mutableListOf<NBSNote>()
            var layer = -1

            while (true) {
                val layerJumps = buffer.unsignedShort // the amount of "jumps" to the next layer with a note (vertical)
                if (layerJumps == 0) break
                layer += layerJumps
                notes.add(NBSNote.read(version, buffer))
            }

            ticks[tick] = notes
        }

        // Read layers
        repeat(layerCount) {
            buffer.string // name
            if (version >= NBSFormat.MODERN_4) {
                buffer.byte // locked (1 = locked, 0 = unlocked)
            }
            buffer.byte // volume (percentage, 0-100)
            if (version >= NBSFormat.MODERN_2) {
                buffer.byte // stereo panning (0 = 2 blocks right, 100 = center, 200 = 2 blocks left)
            }
        }

        // Read custom instruments
        val customInstrumentCount = buffer.byte
        repeat(customInstrumentCount.toInt()) {
            buffer.string // instrument name
            buffer.string // relative file path
            buffer.byte // 0-87, default = 45
            buffer.byte // whether the instrument shows piano key presses in the UI, 0 or 1
        }

        if (buffer.hasRemaining()) {
            System.err.println("${buffer.remaining()} remaining unread bytes after reading NBS file.")
        }

        return SongImpl(
            source = source ?: "<byte stream>",
            tempo = tempo,
            durationInTicks = ticks.size,
            songName = songName,
            author = author,
            originalAuthor = originalAuthor,
            description = description,
            ticks = ticks
        )
    }
}