package com.bluedragonmc.jukebox.command

import com.bluedragonmc.jukebox.api.SongPlayer
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object ResumeCommand {
    fun create(songPlayer: SongPlayer): BrigadierCommand {
        val node = LiteralArgumentBuilder.literal<CommandSource>("resume")
            .requires { source -> source.hasPermission("jukebox.resume") }
            .executes { context ->
                if (context.source is Player) {
                    songPlayer.resume(context.source as Player)
                } else {
                    context.source.sendMessage(
                        Component.text(
                            "You must be a player to use this command!",
                            NamedTextColor.RED
                        )
                    )
                }
                return@executes Command.SINGLE_SUCCESS
            }
        return BrigadierCommand(node)
    }
}