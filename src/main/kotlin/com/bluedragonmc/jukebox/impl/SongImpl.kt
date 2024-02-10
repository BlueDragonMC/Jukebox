package com.bluedragonmc.jukebox.impl

import com.bluedragonmc.jukebox.NBSNote
import com.bluedragonmc.jukebox.api.Song

class SongImpl(
    override val source: String,
    override val tempo: Double,
    override val durationInTicks: Int,
    override val songName: String,
    override val author: String,
    override val originalAuthor: String,
    override val description: String,
    private val ticks: Array<Collection<NBSNote>?>,
) : Song {
    override fun getNotesAt(tick: Int): Collection<NBSNote> {
        return ticks[tick] ?: emptySet()
    }
}