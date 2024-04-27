package com.bluedragonmc.jukebox.api

import com.velocitypowered.api.proxy.Player

interface SongPlayer {
    fun play(song: Song, player: Player, startTimeInTicks: Int = 0)
    fun getCurrentSong(player: Player): SongPlayingStatus?
    fun stop(player: Player)
    fun resume(player: Player): Boolean
    fun pause(player: Player): Boolean
}