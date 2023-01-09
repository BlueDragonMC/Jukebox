package com.bluedragonmc.jukebox

import com.bluedragonmc.jukebox.command.PauseCommand
import com.bluedragonmc.jukebox.command.PlayCommand
import com.bluedragonmc.jukebox.command.ResumeCommand
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
            .map { path -> Song.load(path) }
            .onEach {
                logger.info("Loaded song \"${it.songName}\" by ${it.originalAuthor.ifEmpty { it.author } } (${it.getDuration()})")
            }

        proxyServer.commandManager.apply {
            PlayCommand.create().let {
                register(metaBuilder(it).aliases("play").plugin(this).build(), it)
            }
            PauseCommand.create().let {
                register(metaBuilder(it).aliases("pause").plugin(this).build(), it)
            }
            ResumeCommand.create().let {
                register(metaBuilder(it).aliases("resume").plugin(this).build(), it)
            }
        }

        proxyServer.eventManager.register(this) { e: DisconnectEvent ->
            Song.stop(e.player)
        }

        logger.info("Jukebox plugin successfully initialized.")
    }
}