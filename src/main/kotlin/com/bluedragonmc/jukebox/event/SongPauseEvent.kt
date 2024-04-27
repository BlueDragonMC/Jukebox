package com.bluedragonmc.jukebox.event

import com.bluedragonmc.jukebox.api.Song
import com.velocitypowered.api.proxy.Player

class SongPauseEvent(player: Player, song: Song, val timeInTicks: Int) : SongEvent(player, song)