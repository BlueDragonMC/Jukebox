package com.bluedragonmc.jukebox.util

import com.bluedragonmc.jukebox.api.Song

fun getDurationString(song: Song): String {
    val seconds = (song.durationInTicks / song.tempo).toInt()
    return (seconds / 60).toString().padStart(2, '0') + ":" + (seconds % 60).toString().padStart(2, '0')
}