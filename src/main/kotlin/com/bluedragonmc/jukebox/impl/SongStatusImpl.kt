package com.bluedragonmc.jukebox.impl

import com.bluedragonmc.jukebox.api.Song
import com.bluedragonmc.jukebox.api.SongPlayingStatus
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.scheduler.ScheduledTask
import java.util.concurrent.atomic.AtomicInteger

data class SongStatusImpl(
    override val player: Player,
    override var isPaused: Boolean,
    override val song: Song,
    internal val task: ScheduledTask,
    private val currentTickCounter: AtomicInteger,
) : SongPlayingStatus {
    override val currentTick get() = currentTickCounter.get()
}