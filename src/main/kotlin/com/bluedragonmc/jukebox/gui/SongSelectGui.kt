package com.bluedragonmc.jukebox.gui

import com.bluedragonmc.jukebox.JukeboxPlugin
import com.bluedragonmc.jukebox.Song
import dev.simplix.protocolize.api.inventory.Inventory
import dev.simplix.protocolize.api.item.ItemStack
import dev.simplix.protocolize.data.ItemType
import dev.simplix.protocolize.data.inventory.InventoryType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

object SongSelectGui {

    private fun Component.noItalic() = decoration(TextDecoration.ITALIC, false)

    val inventory by lazy {
        val inventory = Inventory(getInventoryType())
        inventory.title(Component.text("选择音乐"))
        for ((i, song) in JukeboxPlugin.INSTANCE.songs.withIndex()) {
            val itemType = ItemType.values()[ItemType.MUSIC_DISC_11.ordinal + (i % 15)]
            val stack = ItemStack(itemType)
                .displayName(Component.text(song.songName, NamedTextColor.WHITE).noItalic())
            stack.addToLore(Component.empty())
            stack.addToLore(Component.text(song.originalAuthor.ifEmpty { song.author }, NamedTextColor.WHITE).noItalic())
            stack.addToLore(Component.text(song.getDuration(), NamedTextColor.AQUA).noItalic())
            inventory.item(i, stack)
        }
        inventory.onClick { click ->
            click.cancelled(true)
            if (click.slot() in JukeboxPlugin.INSTANCE.songs.indices) {
                val song = JukeboxPlugin.INSTANCE.songs[click.slot()]
                val player = JukeboxPlugin.INSTANCE.proxyServer.getPlayer(click.player().uniqueId())
                if (player.isEmpty) return@onClick
                Song.play(song, player.get())
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
