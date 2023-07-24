package com.github.rushyverse.rtf.listener

import com.github.rushyverse.rtf.RTF
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.manager.GameManager
import com.github.rushyverse.api.extension.toPos
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import org.bukkit.GameMode
import org.bukkit.entity.EntityType
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerRespawnEvent

class GameListener(
    private val plugin: RTF
) : Listener {

    private val games: GameManager = plugin.gameManager
    private val clients: ClientManager by inject(plugin.id)

    @EventHandler
    suspend fun onChangeWorld(event: PlayerChangedWorldEvent) {
        val from = event.from

        if (from.name.contains("rtf")) {
            val game = games.getByWorld(from) ?: return
            val player = event.player

            game.clientLeave(clients.getClient(player) as ClientRTF)
        }
    }

    @EventHandler
    suspend fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        val world = entity.world
        val game = games.getByWorld(world) ?: return

        if (entity.type != EntityType.PLAYER) {
            event.isCancelled = true
        } else {
            val client = clients.getClient(entity.name) as ClientRTF

            if (game.state() != GameState.STARTED) {
                event.isCancelled = true
            } else {
                val team = game.getClientTeam(client) ?: return

                if (team.spawnCuboid.contains(entity.location.toPos())) {
                    event.isCancelled = true
                }
            }
        }
    }
    @EventHandler
    suspend fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val world = player.world
        val game = games.getByWorld(world) ?: return
        val client = clients.getClient(player) as ClientRTF
        val team = game.getClientTeam(client)

        if (team == null) {
            event.respawnLocation = world.spawnLocation
        } else {
            event.respawnLocation = team.spawnPoint
        }
    }

    @EventHandler
    suspend fun onMove(event: PlayerMoveEvent) {
        val player = event.player
        val world = player.world
        val game = games.getByWorld(world) ?: return

        if (event.to.y <= game.mapConfig.limitY) {
            val gameMode = player.gameMode
            if (gameMode == GameMode.SPECTATOR) {
                player.teleport(game.world.spawnLocation)
            } else if (gameMode == GameMode.SURVIVAL) {
                player.health = 0.0
            }
        }
    }

    @EventHandler
    suspend fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val game = games.getByWorld(player.world) ?: return

        if (game.state() != GameState.STARTED) {
            event.isCancelled = true
        } else {
            val client = clients.getClient(player) as ClientRTF
            val block = event.blockPlaced
            val blockLoc = block.location

            if (game.isProtectedLocation(blockLoc)) {
                event.isCancelled = true

                if (block.type.name.contains("WOOL")) {
                    val clientTeam = game.getClientTeam(client) ?: return

                    if (clientTeam.flagPoint == blockLoc.add(.0, 1.0, .0)) {
                        val flagTeam = game.teams.firstOrNull { it.flagMaterial == block.type } ?: return
                        game.clientPlaceFlag(client, flagTeam)
                    }
                }
            } else {
                if (game.mapConfig.allowedBlocks.contains(block.type))
                    event.itemInHand.amount = 64
                else event.isCancelled = player.gameMode != GameMode.CREATIVE
            }
        }
    }


    @EventHandler
    suspend fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val game = games.getByWorld(player.world) ?: return

        if (game.state() != GameState.STARTED) {
            event.isCancelled = true
        } else {
            val client = clients.getClient(player) as ClientRTF
            val type = event.block.type

            when {
                type.name.contains("WOOL") -> {

                    val flagTeam = game.teams.firstOrNull { it.flagMaterial == type } ?: return

                    if (flagTeam == game.getClientTeam(client)) {
                        event.isCancelled = true
                        client.send("cant.break.your.team.flag")
                    } else {
                        event.isDropItems = false
                        game.clientPickupFlag(client, flagTeam)
                    }
                }
                !game.isBlockAllowed(type) -> event.isCancelled = true
                else -> event.isDropItems = false
            }

        }
    }
}