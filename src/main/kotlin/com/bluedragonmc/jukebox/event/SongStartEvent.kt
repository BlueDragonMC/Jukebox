package com.bluedragonmc.jukebox.event

import com.bluedragonmc.jukebox.api.Song
import com.velocitypowered.api.proxy.Player

class SongStartEvent(player: Player, song: Song, val startTimeInTicks: Int) : SongEvent(player, song)