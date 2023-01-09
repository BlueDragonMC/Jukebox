package com.bluedragonmc.jukebox

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

class Song(file: Path) {

    private val version: NBSFormat

    /**
     * The tempo of the song, measured in ticks per second.
     */
    private val tempo: Double

    /**
     * The ticks of this song. Each element contains a tick number and a set of one or more notes to play.
     */
    private val ticks: Array<List<NBSNote>?>

    val songName: String
    val author: String
    val originalAuthor: String
    val description: String

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

    private fun play(proxyServer: ProxyServer, player: Player) {
        val interval = (1000.0 / tempo).toLong()
        val currentTick = AtomicInteger(0)
        lateinit var task: ScheduledTask
        task = proxyServer.scheduler.buildTask(JukeboxPlugin.INSTANCE) {
            if (status[player]?.isPaused == true) return@buildTask
            val tick = currentTick.getAndIncrement()
            if (tick >= ticks.size) {
                // Song has ended
                task.cancel()
                status.remove(player)
                return@buildTask
            }

            ticks[tick]?.forEach {
                it.playTo(player)
            }
        }.repeat(Duration.ofMillis(interval)).schedule()
        status[player] = Status(false, task, this)
    }

    fun getDuration(): String {
        val seconds = (ticks.size / tempo).toInt()
        return (seconds / 60).toString().padStart(2, '0') + ":" + (seconds % 60).toString().padStart(2, '0')
    }

    data class Status(var isPaused: Boolean, val task: ScheduledTask, val song: Song)

    companion object {
        val status = mutableMapOf<Player, Status>()

        fun pause(player: Player) {
            status[player]?.isPaused = true
        }

        fun resume(player: Player) {
            status[player]?.isPaused = false
        }

        fun stop(player: Player) {
            status[player]?.task?.cancel()
            status.remove(player)
        }

        fun getCurrentSong(player: Player): Song? {
            return status[player]?.song
        }

        fun play(song: Song, player: Player) {
            stop(player)
            song.play(JukeboxPlugin.INSTANCE.proxyServer, player)
        }

        fun load(path: Path) = Song(path)
        fun load(name: String) = Song(JukeboxPlugin.INSTANCE.dataDirectory.resolve("songs/$name"))
    }
}