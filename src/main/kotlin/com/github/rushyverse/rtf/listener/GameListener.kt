package com.github.rushyverse.rtf.listener

import com.github.rushyverse.api.extension.event.cancel
import com.github.rushyverse.api.extension.isWool
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.player.getTypedClient
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.game.GameManager
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.entity.EntityType
import org.bukkit.entity.Villager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*

class GameListener : Listener {

    private val plugin : RTFPlugin by inject(RTFPlugin.ID)
    private val games: GameManager by inject(RTFPlugin.ID)
    private val clients: ClientManager by inject(RTFPlugin.ID)

    @EventHandler
    suspend fun onChangeWorld(event: PlayerChangedWorldEvent) {
        val from = event.from

        if (from.name.contains("rtf")) {
            val game = games.getByWorld(from) ?: return
            val player = event.player

            game.clientLeave(clients.getTypedClient(player))
        }
    }

    @EventHandler
    suspend fun onClickVillager(event: PlayerInteractEntityEvent) {
        val player = event.player
        val game = games.getByWorld(player.world) ?: return
        val entity = event.rightClicked
        if (entity is Villager) {
            val client = clients.getTypedClient<ClientRTF>(player)

            event.cancel()

            plugin.kitsGui.open(client)
        }
    }

    @EventHandler
    suspend fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.player
        val game = games.getByWorld(player.world) ?: return
        val client = clients.getTypedClient<ClientRTF>(player)
        val team = game.getClientTeam(client) ?: return
        val killer = player.killer

        event.keepInventory = true

        if (game.state() != GameState.STARTED) {
            player.teleport(team.spawnPoint)
            event.deathMessage(null)
            return
        }

        client.stats.incDeaths()

        val deathMessage = StringBuilder(player.name)
        deathMessage.append(" ")

        if (killer != null) {
            val clientKiller = clients.getTypedClient<ClientRTF>(killer)

            clientKiller.stats.incKills()
            clientKiller.reward(game.config.rewards.kill)

            deathMessage.append("was killed by ${killer.name}")
        } else {
            deathMessage.append("fell into the void")
        }

        val wool = player.inventory.firstOrNull { it?.type?.isWool() == true }?.type
        if (wool != null) {
            val flagTeam = game.teams.firstOrNull { it.flagMaterial == wool } ?: return
            game.clientDeathWithFlag(client, flagTeam)

            deathMessage.append(" with the ${flagTeam.type.name} flag!")
        }

        event.deathMessage(Component.text(deathMessage.toString()))
    }

    @EventHandler
    suspend fun onEntityDamage(event: EntityDamageEvent) {
        val entity = event.entity
        val world = entity.world
        val game = games.getByWorld(world) ?: return

        if (entity.type != EntityType.PLAYER) {
            event.isCancelled = true
        } else {
            val client = clients.getTypedClient<ClientRTF>(entity.name)

            if (game.state() != GameState.STARTED) {
                event.isCancelled = true
            } else {
                val team = game.getClientTeam(client) ?: return

                if (team.spawnCuboid.isInArea(entity.location)) {
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
        val client = clients.getTypedClient<ClientRTF>(player)
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
            val client = clients.getTypedClient<ClientRTF>(player)
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
            val client = clients.getTypedClient<ClientRTF>(player)
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
