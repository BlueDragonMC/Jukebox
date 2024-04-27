package com.bluedragonmc.jukebox.event

import com.bluedragonmc.jukebox.api.Song
import com.velocitypowered.api.proxy.Player

class SongEndEvent(player: Player, song: Song) : SongEvent(player, song)