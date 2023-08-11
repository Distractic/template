package com.github.rushyverse.rtf.listener

import com.github.rushyverse.api.extension.event.cancel
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.gui.commons.GUI
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent

class GUIListener(
    private val listOfGui: Set<GUI>
) : ListenerRTF() {

    @EventHandler
    suspend fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem ?: return

        val player = event.whoClicked as Player
        if (!isRTFWorld(player.world)) return

        val client = clients.getClient(player) as ClientRTF
        val guiFind = listOfGui.firstOrNull { it.viewers.contains(client) } ?: return

        event.cancel()
        guiFind.onClick(client, item, event.click)
    }

    @EventHandler
    suspend fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as Player

        if (!isRTFWorld(player.world)) return

        clients.getClientOrNull(player)?.apply {
            listOfGui.forEach {
                if (it.viewers.contains(this)) {
                    it.close(this)
                }
            }
        }
    }
}
