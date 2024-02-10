package com.bluedragonmc.jukebox.command

import com.bluedragonmc.jukebox.gui.SongSelectGui
import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import dev.simplix.protocolize.api.Protocolize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object PlayCommand {
    fun create(songSelectGui: SongSelectGui): BrigadierCommand {
        val node = LiteralArgumentBuilder.literal<CommandSource>("play")
            .requires { source -> source.hasPermission("jukebox.play") }
            .executes { context ->
                if (context.source is Player) {
                    Protocolize.playerProvider().player((context.source as Player).uniqueId)
                        ?.openInventory(songSelectGui.inventory)
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