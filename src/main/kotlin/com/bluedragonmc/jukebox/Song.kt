package com.bluedragonmc.jukebox

import com.bluedragonmc.jukebox.event.SongEndEvent
import com.bluedragonmc.jukebox.event.SongPauseEvent
import com.bluedragonmc.jukebox.event.SongResumeEvent
import com.bluedragonmc.jukebox.event.SongStartEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo

class Song(file: Path) {

    private val version: NBSFormat

    /**
     * The tempo of the song, measured in ticks per second.
     */
    val tempo: Double

    /**
     * The ticks of this song. Each element contains a tick number and a set of one or more notes to play.
     */
    private val ticks: Array<List<NBSNote>?>

    val fileName: String = file.relativeTo(JukeboxPlugin.INSTANCE.dataDirectory).toString()

    val songName: String
    val author: String
    val originalAuthor: String
    val description: String
    val durationInTicks: Int get() = ticks.size

    init {
        val buffer = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        val firstBytes =
            buffer.unsignedShort // The first two bytes could be 00 (new NBS format) or the length of the song (NBS classic format)
        val songLength: Short
        if (firstBytes == 0) {
            // New OpenNBS format
            val versionByte = buffer.byte // the new OpenNBS format version (0-5)
            version = NBSFormat.modern(versionByte)
            buffer.byte // the index at which custom instruments start (vanilla instrument count)
            if (version >= NBSFormat.MODERN_3) {
                songLength = buffer.short // song length in ticks
            } else {
                songLength = -1 // song length must be calculated based on the song's layers
            }
        } else {
            version = NBSFormat.CLASSIC
            songLength = firstBytes.toShort()
        }
        val layerCount = buffer.unsignedShort
        songName = buffer.string.ifEmpty { file.nameWithoutExtension }
        author = buffer.string.ifEmpty { "Unknown" }
        originalAuthor = buffer.string
        description = buffer.string
        tempo = buffer.unsignedShort / 100.0
        buffer.byte // auto-saving enabled
        buffer.byte // auto-saving duration
        buffer.byte // time signature
        buffer.int // minutes spent editing
        buffer.int // total left clicks
        buffer.int // total right clicks
        buffer.int // total note blocks added
        buffer.int // total note blocks removed
        buffer.string // .midi file name
        if (version >= NBSFormat.MODERN_4) {
            buffer.byte // loop
            buffer.byte // max loop count
            buffer.unsignedShort // loop start tick
        }

        // Read the song's notes
        var tick = -1
        this.ticks = arrayOfNulls(songLength + 1)

        while (true) {
            val tickJumps = buffer.unsignedShort // the amount of "jumps" to the next tick with a note (horizontal)
            if (tickJumps == 0) break
            tick += tickJumps

            // Read the layers at this tick
            val notes = mutableListOf<NBSNote>()
            var layer = -1

            while (true) {
                val layerJumps = buffer.unsignedShort // the amount of "jumps" to the next layer with a note (vertical)
                if (layerJumps == 0) break
                layer += layerJumps
                notes.add(NBSNote.read(version, buffer))
            }

            ticks[tick] = notes
        }

        // Read layers
        repeat(layerCount) {
            buffer.string // name
            if (version >= NBSFormat.MODERN_4) {
                buffer.byte // locked (1 = locked, 0 = unlocked)
            }
            buffer.byte // volume (percentage, 0-100)
            if (version >= NBSFormat.MODERN_2) {
                buffer.byte // stereo panning (0 = 2 blocks right, 100 = center, 200 = 2 blocks left)
            }
        }

        // Read custom instruments
        val customInstrumentCount = buffer.byte
        repeat(customInstrumentCount.toInt()) {
            buffer.string // instrument name
            buffer.string // relative file path
            buffer.byte // 0-87, default = 45
            buffer.byte // whether the instrument shows piano key presses in the UI, 0 or 1
        }

        if (buffer.hasRemaining()) {
            JukeboxPlugin.INSTANCE.logger.fine("${buffer.remaining()} remaining unread bytes after reading NBS file.")
        }
    }

    private fun play(proxyServer: ProxyServer, player: Player, startTimeInTicks: Int = 0) {
        val interval = (1000.0 / tempo).toLong()
        val currentTick = AtomicInteger(startTimeInTicks)
        lateinit var task: ScheduledTask
        task = proxyServer.scheduler.buildTask(JukeboxPlugin.INSTANCE) {
            if (status[player]?.isPaused == true) return@buildTask
            val tick = currentTick.getAndIncrement()
            if (tick >= ticks.size) {
                // Song has ended
                task.cancel()
                status.remove(player)
                proxyServer.eventManager.fireAndForget(SongEndEvent(player, this))
                return@buildTask
            }

            ticks[tick]?.forEach {
                it.playTo(player)
            }
        }.repeat(Duration.ofMillis(interval)).schedule()
        status[player] = Status(false, this, task, currentTick)
        proxyServer.eventManager.fireAndForget(SongStartEvent(player, this, startTimeInTicks))
    }

    fun getDuration(): String {
        val seconds = (ticks.size / tempo).toInt()
        return (seconds / 60).toString().padStart(2, '0') + ":" + (seconds % 60).toString().padStart(2, '0')
    }

    data class Status(
        var isPaused: Boolean,
        val song: Song,
        internal val task: ScheduledTask,
        private val currentTick: AtomicInteger,
    ) {
        val currentTimeInTicks get() = currentTick.get()
        val lengthInTicks get() = song.ticks.size
        val lengthInSeconds get() = song.ticks.size / song.tempo
        val notes get() = song.ticks
    }

    companion object {
        val status = mutableMapOf<Player, Status>()

        fun pause(player: Player) {
            status[player]?.let { status ->
                JukeboxPlugin.INSTANCE.proxyServer.eventManager.fireAndForget(
                    SongPauseEvent(
                        player,
                        status.song,
                        status.currentTimeInTicks
                    )
                )
            }
            status[player]?.isPaused = true
        }

        fun resume(player: Player) {
            status[player]?.let { status ->
                JukeboxPlugin.INSTANCE.proxyServer.eventManager.fireAndForget(
                    SongResumeEvent(
                        player,
                        status.song,
                        status.currentTimeInTicks
                    )
                )
            }
            status[player]?.isPaused = false
        }

        fun stop(player: Player) {
            status[player]?.song?.let { song ->
                JukeboxPlugin.INSTANCE.proxyServer.eventManager.fireAndForget(SongEndEvent(player, song))
            }
            status[player]?.task?.cancel()
            status.remove(player)
        }

        fun getCurrentSong(player: Player): Status? {
            return status[player]
        }

        fun play(song: Song, player: Player, startTimeInTicks: Int = 0) {
            stop(player)
            song.play(JukeboxPlugin.INSTANCE.proxyServer, player, startTimeInTicks)
        }

        /**
         * Loads a song from an arbitrary path
         */
        fun load(path: Path) = Song(path)

        /**
         * Loads a song relative to the Jukebox plugin's `data/songs` directory
         */
        fun load(name: String) = loadRelative(Path.of("songs", name))

        /**
         * Loads a song relative to the Jukebox plugin's data directory
         */
        fun loadRelative(path: Path) = Song(JukeboxPlugin.INSTANCE.dataDirectory.resolve(path))
    }
}