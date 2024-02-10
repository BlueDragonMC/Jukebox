package com.bluedragonmc.jukebox.api

import com.bluedragonmc.jukebox.NBSNote

interface Song {

    /**
     * An identifier for the source of this song.
     * In the default SongLoader implementation, this is
     * the path of the NBS file relative to the plugin's
     * data directory.
     */
    val source: String

    /**
     * The tempo of the song, measured in ticks per second.
     */
    val tempo: Double

    val durationInTicks: Int

    val durationInSeconds: Double
        get() = durationInTicks / tempo

    val songName: String

    val author: String

    val originalAuthor: String

    val description: String

    fun getNotesAt(tick: Int): Collection<NBSNote>
}