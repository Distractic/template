package com.github.rushyverse.rtf.listener

import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.game.GameManager
import org.bukkit.World
import org.bukkit.event.Listener

/**
 * A basic class to manage events related to the RTF.
 * It provides common useful verification methods and accessors that can be used in multiple sub-listeners.
 */
open class ListenerRTF : Listener {

    /**
     * Access to the plugin.
     */
    protected val plugin: RTFPlugin by inject(RTFPlugin.ID)

    /**
     * Access to the manager of games.
     */
    protected val games : GameManager by inject(RTFPlugin.ID)

    /**
     * Access to the manager of clients.
     */
    protected val clients : ClientManager by inject(RTFPlugin.ID)

    /**
     * To find out if the current world is a world related to RTF.
     * @param world The world to check.
     * @return true if the world is a world related to RTF, otherwise false.
     */
    fun isRTFWorld(world: World) = world.name.contains("rtf")
}
