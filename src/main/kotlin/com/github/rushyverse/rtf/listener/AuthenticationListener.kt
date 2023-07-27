package com.github.rushyverse.rtf.listener

import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.game.GameManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

class AuthenticationListener : Listener {

    private val games : GameManager by inject(RTFPlugin.ID)
    private val clients : ClientManager by inject(RTFPlugin.ID)

    @EventHandler
    suspend fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val world = player.world
        val game = games.getByWorld(world)

        game?.apply {
            clientLeave(clients.getClient(player) as ClientRTF)
        }

        // Save the rtf client stats to the database

        event.quitMessage(null)
    }
}