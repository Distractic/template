package com.github.rushyverse.rtf.listener

import com.github.rushyverse.api.extension.event.cancel
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.game.GameManager
import com.github.rushyverse.rtf.gui.commons.GUI
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

class GUIListener(
    val plugin: RTFPlugin,
    val listOfGui: Set<GUI>
) : Listener {

    val clients: ClientManager by inject(plugin.id)
    val games: GameManager by inject(plugin.id)

    @EventHandler
    suspend fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player
        val world = player.world
        if (!world.name.contains("rtf")) return
        val item = event.currentItem ?: return
        val client = clients.getClient(player) as ClientRTF

        val guiFind = listOfGui.firstOrNull { it.viewers.contains(client) } ?: return

        event.cancel()
        guiFind.onClick(client, item, event.click)
    }

    @EventHandler
    suspend fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player
        val world = player.world
        if (!world.name.contains("rtf")) return

        clients.getClientOrNull(player)?.apply {
            listOfGui.forEach {
                if (it.viewers.contains(this)) {
                    it.close(this)
                }
            }
        }

    }
}