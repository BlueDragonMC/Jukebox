package com.bluedragonmc.jukebox.impl

import com.bluedragonmc.jukebox.api.Note
import com.bluedragonmc.jukebox.api.Song
import com.bluedragonmc.jukebox.api.SongPlayer
import com.bluedragonmc.jukebox.api.SongPlayingStatus
import com.bluedragonmc.jukebox.event.SongEndEvent
import com.bluedragonmc.jukebox.event.SongPauseEvent
import com.bluedragonmc.jukebox.event.SongResumeEvent
import com.bluedragonmc.jukebox.event.SongStartEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import dev.simplix.protocolize.api.Location
import dev.simplix.protocolize.api.Protocolize
import dev.simplix.protocolize.api.SoundCategory
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.pow

class SongPlayerImpl(private val plugin: Any, private val proxyServer: ProxyServer) : SongPlayer {

    private val statuses = mutableMapOf<Player, SongStatusImpl>()

    override fun pause(player: Player): Boolean {
        statuses[player]?.let { status ->
            proxyServer.eventManager.fireAndForget(
                SongPauseEvent(
                    player,
                    status.song,
                    status.currentTick
                )
            )
        }
        statuses[player]?.isPaused = true
        return statuses.containsKey(player)
    }

    override fun resume(player: Player): Boolean {
        statuses[player]?.let { status ->
            proxyServer.eventManager.fireAndForget(
                SongResumeEvent(
                    player,
                    status.song,
                    status.currentTick
                )
            )
        }
        statuses[player]?.isPaused = false
        return statuses.containsKey(player)
    }

    override fun stop(player: Player) {
        statuses[player]?.task?.cancel()
        statuses[player]?.song?.let { song ->
            proxyServer.eventManager.fireAndForget(SongEndEvent(player, song))
        }
        statuses.remove(player)
    }

    override fun getCurrentSong(player: Player): SongPlayingStatus? {
        return statuses[player]
    }

    override fun play(song: Song, player: Player, startTimeInTicks: Int) {
        stop(player)
        val interval = (1000.0 / song.tempo).toLong()
        val currentTick = AtomicInteger(startTimeInTicks)
        lateinit var task: ScheduledTask
        task = proxyServer.scheduler.buildTask(plugin) { _ ->
            if (statuses[player]?.isPaused == true) return@buildTask
            val tick = currentTick.getAndIncrement()
            if (tick >= song.durationInTicks) {
                // Song has ended
                task.cancel()
                stop(player)
                return@buildTask
            }

            song.getNotesAt(tick).forEach { note ->
                playNote(note, player)
            }
        }.repeat(Duration.ofMillis(interval)).schedule()
        statuses[player] = SongStatusImpl(player, false, song, task, currentTick)
        proxyServer.eventManager.fireAndForget(SongStartEvent(player, song, startTimeInTicks))
    }

    fun playNote(note: Note, player: Player) {
        val wrappedPlayer = Protocolize.playerProvider().player(player.uniqueId) ?: return
        val position = if (note.pan != null) {
            // 0 => 2 blocks to the right
            // 100 => center
            // 200 => 2 blocks to the left
            if (note.pan == 100.toByte()) wrappedPlayer.location()
            else {
                val adjusted =
                    -(note.pan!! - 100) * (90f / 100f) * (PI / 180.0) // Convert the pan value into a rotation amount in radians
                val direction =
                    note.getHorizontalDirection(wrappedPlayer.location().yaw(), wrappedPlayer.location().pitch())
                val offset = direction.rotate(adjusted)
                Location(
                    wrappedPlayer.location().x() + offset.x,
                    wrappedPlayer.location().y(),
                    wrappedPlayer.location().z() + offset.y,
                    0f,
                    0f
                )
            }
        } else {
            wrappedPlayer.location()
        }

        try {
            val finalKey = note.key + (note.pitch ?: 0) / 100
            wrappedPlayer.playSound(
                position,
                note.getSound(),
                SoundCategory.RECORDS,
                (note.velocity?.toFloat() ?: 100f) / 100f,
                2f.pow((finalKey - 45) / 12f)
            )
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            player.sendMessage(
                Component.text(
                    "There was an error playing this song! Please contact a server administrator.",
                    NamedTextColor.RED
                )
            )
        }
    }
}