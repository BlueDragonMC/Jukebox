package com.bluedragonmc.jukebox.gui

import com.bluedragonmc.jukebox.JukeboxPlugin
import com.bluedragonmc.jukebox.api.SongPlayer
import com.bluedragonmc.jukebox.util.getDurationString
import dev.simplix.protocolize.api.chat.ChatElement
import dev.simplix.protocolize.api.inventory.Inventory
import dev.simplix.protocolize.api.item.ItemStack
import dev.simplix.protocolize.data.ItemType
import dev.simplix.protocolize.data.inventory.InventoryType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class SongSelectGui(private val songPlayer: SongPlayer) {

    private fun Component.noItalic() = decoration(TextDecoration.ITALIC, false)

    private fun Component.asChatElement() = ChatElement.of(this)

    val inventory by lazy {
        val inventory = Inventory(getInventoryType())
        inventory.title(Component.text("Select Song").asChatElement())
        for ((i, song) in JukeboxPlugin.INSTANCE.songs.withIndex()) {
            val itemType = ItemType.entries[ItemType.MUSIC_DISC_11.ordinal + (i % 15)]
            val stack = ItemStack(itemType)
            stack.displayName(Component.text(song.songName, NamedTextColor.WHITE).noItalic().asChatElement())
            stack.addToLore(Component.empty().asChatElement())
            stack.addToLore(
                Component.text(song.originalAuthor.ifEmpty { song.author }, NamedTextColor.WHITE).noItalic().asChatElement()
            )
            stack.addToLore(Component.text(getDurationString(song), NamedTextColor.AQUA).noItalic().asChatElement())
            inventory.item(i, stack)
        }
        inventory.onClick { click ->
            click.cancelled(true)
            if (click.slot() in JukeboxPlugin.INSTANCE.songs.indices) {
                val song = JukeboxPlugin.INSTANCE.songs[click.slot()]
                val player = JukeboxPlugin.INSTANCE.proxyServer.getPlayer(click.player().uniqueId())
                if (player.isEmpty) return@onClick
                songPlayer.play(song, player.get())
                click.player().closeInventory()
            }
        }
        return@lazy inventory
    }

    private fun getInventoryType(): InventoryType {
        val slots = JukeboxPlugin.INSTANCE.songs.size
        return when {
            slots > 46 -> InventoryType.GENERIC_9X6
            slots > 36 -> InventoryType.GENERIC_9X5
            slots > 27 -> InventoryType.GENERIC_9X4
            slots > 18 -> InventoryType.GENERIC_9X3
            slots > 9 -> InventoryType.GENERIC_9X2
            else -> InventoryType.GENERIC_9X1
        }
    }
}