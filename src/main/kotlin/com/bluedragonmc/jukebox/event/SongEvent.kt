package com.bluedragonmc.jukebox.event

import com.bluedragonmc.jukebox.Song
import com.velocitypowered.api.proxy.Player

abstract class SongEvent(val player: Player, val song: Song)
