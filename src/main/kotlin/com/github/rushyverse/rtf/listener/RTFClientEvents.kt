package com.github.rushyverse.rtf.listener

import com.github.rushyverse.rtf.RTF
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.api.player.*
import net.kyori.adventure.text.Component
import java.util.concurrent.atomic.AtomicReference

class RTFClientEvents(
    val plugin: RTF
) : PluginClientEvents() {


    override suspend fun onJoin(client: Client, joinMessage: AtomicReference<Component?>) {
        val clientRTF = client as ClientRTF

        if (plugin.config.dedicatedServer) {

            // TODO Ask to Redis the game index


        }
        joinMessage.set(null)
    }

    override suspend fun onQuit(client: Client, quitMessage: AtomicReference<Component?>) {

        val player = client.requirePlayer()
        val world = player.world
        val game = plugin.gameManager.getByWorld(world)

        game?.clientLeave(client as ClientRTF)

        // Save the rtf client stats to the database

        quitMessage.set(null)
    }
}