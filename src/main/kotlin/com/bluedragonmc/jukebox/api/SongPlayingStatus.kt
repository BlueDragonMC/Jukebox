package com.bluedragonmc.jukebox.api

import com.velocitypowered.api.proxy.Player

interface SongPlayingStatus {
    var isPaused: Boolean
    val song: Song
    val player: Player
    val currentTick: Int
}