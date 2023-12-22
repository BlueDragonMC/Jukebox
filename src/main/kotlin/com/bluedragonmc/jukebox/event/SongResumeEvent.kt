package com.bluedragonmc.jukebox.event

import com.bluedragonmc.jukebox.Song
import com.velocitypowered.api.proxy.Player

class SongResumeEvent(player: Player, song: Song, val timeInTicks: Int) : SongEvent(player, song)