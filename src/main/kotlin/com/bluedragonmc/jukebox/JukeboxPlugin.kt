package com.bluedragonmc.jukebox

import com.bluedragonmc.jukebox.api.Song
import com.bluedragonmc.jukebox.api.SongLoader
import com.bluedragonmc.jukebox.api.SongPlayer
import com.bluedragonmc.jukebox.command.PauseCommand
import com.bluedragonmc.jukebox.command.PlayCommand
import com.bluedragonmc.jukebox.command.ResumeCommand
import com.bluedragonmc.jukebox.gui.SongSelectGui
import com.bluedragonmc.jukebox.impl.NBSSongLoader
import com.bluedragonmc.jukebox.impl.SongPlayerImpl
import com.bluedragonmc.jukebox.util.getDurationString
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.simplix.protocolize.api.Protocolize
import dev.simplix.protocolize.data.listeners.PlayerPositionLookListener
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.io.path.*

@Plugin(
    id = "bluedragon-jukebox",
    name = "jukebox",
    url = "https://bluedragonmc.com",
    authors = ["FluxCapacitor2"],
    dependencies = [Dependency(id = "protocolize", optional = false)]
)
class JukeboxPlugin @Inject constructor(
    internal val proxyServer: ProxyServer,
    internal val logger: Logger,
    @DataDirectory internal val dataDirectory: Path,
) {

    companion object {
        lateinit var INSTANCE: JukeboxPlugin
    }

    lateinit var songs: List<Song>

    // Load the default SongLoader and SongPlayer implementations for internal use by the plugin
    private val songLoader = NBSSongLoader()
    private val songPlayer = SongPlayerImpl(this, proxyServer)

    fun getSongLoader(): SongLoader = songLoader
    fun getSongPlayer(proxyServer: ProxyServer, plugin: Any): SongPlayer = SongPlayerImpl(plugin, proxyServer)

    @Subscribe
    fun onInit(event: ProxyInitializeEvent) {
        INSTANCE = this

        Protocolize.listenerProvider().registerListener(PlayerPositionLookListener())

        val songsFolder = dataDirectory.resolve("songs")
        if (!songsFolder.exists()) {
            songsFolder.createDirectories()
        }

        songs = songsFolder.listDirectoryEntries()
            .filter { path -> path.isRegularFile() && path.extension == "nbs" }
            .map { path -> songLoader.load(path.name, path.readBytes()) }
            .onEach {
                val author = it.originalAuthor.ifEmpty { it.author }
                logger.info("Loaded song \"${it.songName}\" by $author (${getDurationString(it)})")
            }

        val songSelect = SongSelectGui(songPlayer)

        proxyServer.commandManager.register(PlayCommand.create(songSelect))
        proxyServer.commandManager.register(PauseCommand.create(songPlayer))
        proxyServer.commandManager.register(ResumeCommand.create(songPlayer))

        logger.info("Jukebox plugin successfully initialized.")
    }

    @Subscribe
    fun onPlayerLeave(event: DisconnectEvent) {
        songPlayer.stop(event.player)
    }
}