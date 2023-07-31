package com.github.rushyverse.rtf.manager

import com.github.rushyverse.rtf.RTF
import com.github.rushyverse.rtf.RTF.Companion.translationsProvider
import com.github.rushyverse.rtf.TeamRTF
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.config.MapConfig
import com.github.rushyverse.rtf.config.RTFConfig
import com.github.shynixn.mccoroutine.bukkit.scope
import com.github.rushyverse.api.extension.toPos
import com.github.rushyverse.api.game.GameData
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.schedule.SchedulerTask
import net.kyori.adventure.text.Component.text
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Game(
    val plugin: RTF,
    val id: Int,
    val world: World,
    val config: RTFConfig,
    val mapConfig : MapConfig
) {
    val data = GameData("rtf", id)

    val players: Collection<Player>
        get() = world.players

    val gameTask = SchedulerTask(plugin.scope, 1.seconds)

    val minPlayers = 2 // to delete, use config instead

    val teams: MutableList<TeamRTF>

    val createdTime = System.currentTimeMillis()
    var startedTime: Long = 0 // value set when the game starts

    private val clients: ClientManager by inject(plugin.id)
    private val manager: GameManager by inject(plugin.id)

    init {
        teams = mutableListOf()
        for (teamConfig in mapConfig.teams) {
            val team = TeamRTF(teamConfig, world)
            teams.add(team)
        }
    }

    fun state(): GameState = data.state

    private fun sendBasicKit(player: Player) {
        val inv = player.inventory

        inv.helmet = ItemStack(Material.LEATHER_HELMET)
        inv.chestplate = ItemStack(Material.LEATHER_CHESTPLATE)
        inv.leggings = ItemStack(Material.LEATHER_LEGGINGS)
        inv.boots = ItemStack(Material.LEATHER_BOOTS)
        inv.addItem(ItemStack(Material.IRON_SWORD))
        inv.addItem(ItemStack(Material.IRON_PICKAXE))
        inv.addItem(ItemStack(Material.SMOOTH_SANDSTONE).apply { amount = 64 })

        player.sendActionBar(text("§EKit actuel : §6Basique"))
    }

    suspend fun start(force: Boolean = false) {
        if (force) {
            data.state = GameState.STARTED

            broadcast("game.message.started")

            teams.forEach { team ->
                team.members.forEach { member ->
                    member.requirePlayer().apply {
                        teleport(team.spawnPoint)
                        sendBasicKit(this)
                    }
                }
            }

            startedTime = System.currentTimeMillis()

            gameTask.add("scoreboardUpdate") { gameTimeTask() }
            gameTask.run()
        } else {
            val time = AtomicInteger(5)

            data.state = GameState.STARTING

            startingTask.add { startingTask(this, time) }
            startingTask.run()
        }

        plugin.saveUpdate(data)
    }

    private suspend fun startingTask(task: SchedulerTask.Task, atomicTime: AtomicInteger) {
        val time = atomicTime.get()
        if (time == 0) {
            start(true)
            task.remove() // end the repeating task
            return
        }
        broadcast("game.message.starting", listOf(time))
        atomicTime.set(time - 1)
    }

    /*
     * Represents the task during the game.
     * Update every seconds the scoreboard with the current
     * formatted time since the game was started.
     */
    private suspend fun gameTimeTask() {
        val time = (System.currentTimeMillis() - startedTime) / 1000
        val timeFormatted = time.toInt().formatSeconds()
        for (player in players) {
            val client = clients.getClient(player) as ClientRTF
            GameScoreboard.update(client, this, timeFormatted)
        }
    }

    fun getClientTeam(client: ClientRTF): TeamRTF? {
        return teams.firstOrNull { it.members.contains(client) }
    }

    suspend fun clientSpectate(client: ClientRTF) {
        val player = client.requirePlayer()

        client.fastBoard.apply {
            updateTitle("§B§LRTF #${data.id}")

            val teamsList = mutableListOf<String>()
            for (team in teams){
                val teamName = team.type.name(translationsProvider, client.locale)
                val state = team.flagStolenState
                teamsList.add(teamName)
            }

            updateLines(
                "",
                "§D§L§DSPECTATE MODE",
                "Use §E/rtf join",
                "",
                *teamsList.toTypedArray(),
                "",
                "rushy.space"
            )
        }

        player.gameMode = GameMode.SPECTATOR
        player.inventory.clear()
        player.teleport(world.spawnLocation)

        if (startedTime == 0L) // Avoid over-using
            GameScoreboard.update(client, this)

        data.players = players.size

        plugin.saveUpdate(data)
    }

    private fun findJoinTeam(): TeamRTF {
        val minPlayers = teams.minOf { it.members.size }
        val smallestTeams = teams.filter { it.members.size == minPlayers }

        return smallestTeams[Random.nextInt(smallestTeams.size)]
    }

    suspend fun clientJoin(client: ClientRTF) {
        val player = client.requirePlayer()
        val joinedTeam = findJoinTeam().also {
            player.teleport(it.spawnPoint)
            player.gameMode = GameMode.SURVIVAL
            it.members.add(client)
        }

        broadcast("player.join.team", listOf(player.name, joinedTeam.type.name))

        if (startedTime == 0L)
            GameScoreboard.update(client, this)
    }

    suspend fun clientLeave(client: ClientRTF) {
        val playersSize = players.size
        val team = teams.firstOrNull { it.members.contains(client) }
        team?.members?.remove(client)

        data.players = playersSize

        // Leave while game is starting and the current number of players is not reached
        if (playersSize < minPlayers) {

            when (data.state) {
                GameState.STARTING -> {
                    startingTask.tasks[0].remove()

                    broadcast("game.message.client.leave.starting")
                    data.state = GameState.WAITING
                }

                GameState.STARTED -> {
                    teams.forEach {
                        if (it.members.isEmpty()) {
                            end(GameEndCause.TEAM_EMPTY)
                        }
                    }
                }

                else -> {}
            }

        }

        plugin.saveUpdate(data)
    }

    suspend fun clientPickupFlag(client: ClientRTF, flagTeam: TeamRTF) {
        val player = client.requirePlayer()

        flagTeam.flagStolenState = true
        client.stats.flagAttempts += 1

        broadcast("player.pickup.flag", listOf(player.name, flagTeam.type.name))
        client.reward(config.rewards.flagPickUp)

        player.addPotionEffect(
            PotionEffect(PotionEffectType.SPEED, 6000, 1)
        )

        val woolItem = ItemStack(flagTeam.flagMaterial)
        player.inventory.apply {
            clear()
            repeat(size) {
                setItem(it, woolItem)
            }
        }

        client.send("you.have.flag")
    }

    fun clientDeathWithFlag(client: ClientRTF, flagTeam: TeamRTF) {
        client.requirePlayer().apply {
            inventory.clear()
            sendBasicKit(this)
            removePotionEffect(PotionEffectType.SPEED)
        }

        flagTeam.flagStolenState = false
    }

    suspend fun clientPlaceFlag(client: ClientRTF, flagTeam: TeamRTF) {
        val player = client.requirePlayer()

        client.stats.flagPlaces += 1
        flagTeam.flagStolenState = false

        broadcast("player.place.flag", listOf(player.name, flagTeam.type.name))
        client.reward(config.rewards.flagPlace)

        end(team)
    }

    fun giveWinRewards(winTeam: TeamRTF) {
        teams.forEach { team ->
            val winner = team == winTeam
            team.members.forEach { member ->
                member.reward(
                    if (winner) config.rewards.win
                    else config.rewards.lose
                )
            }
        }
    }

    /**
     * Ends this game.
     * The game state is set to ENDING while players are teleported and the world is destroyed.
     */
    suspend fun end(winTeam: TeamRTF?) {
        data.state = GameState.ENDING
        gameTask.cancelAndJoin()
        manager.sharedGameData.saveUpdate(data)

        if (winTeam == null) {
            broadcast("game.end.other")
            ejectPlayersAndDestroy()
        } else {
            broadcast("game.end.win", listOf(winTeam.type.name))
            giveWinRewards(winTeam)

            val taskFireworks = BukkitRunnable {
                winTeam.members.forEach {
                    it.player?.location?.spawnRandomFirework()
                }
            }.runTaskTimer(plugin, 0, 15L)

            // Ending the previous task, eject the players and destroy the game after 10s
            BukkitRunnable {
                taskFireworks.cancel()
                ejectPlayersAndDestroy()
            }.runTaskLater(plugin, 200L)
        }
    }

    private fun ejectPlayersAndDestroy() {
        for (player in players) {
            player.performCommand(config.game.backToHubCommand)
        }

        BukkitRunnable {
            plugin.launch {
                manager.removeGameAndDeleteWorld(this@Game)
            }
        }.runTaskLater(plugin, 40L)
    }

    suspend fun broadcast(key: String, args: List<Any> = emptyList()) = plugin.broadcast(world, key, args)

    fun isProtectedLocation(location: Location): Boolean {
        val pos = location.toPos()
        for (team in teams){
            if (team.spawnCuboid.contains(pos) || team.flagCuboid.contains(pos)){
                return true
            }
        }
        return false
    }

    fun isBlockAllowed(blockType: Material): Boolean {
        return mapConfig.allowedBlocks.contains(blockType)
    }

private fun Location.spawnRandomFirework() {
    val effect = FireworkEffect.builder()
        .with(FireworkEffect.Type.entries.toTypedArray().random())
        .withColor(Color.fromRGB(nextInt(256), nextInt(256), nextInt(256)))
        .withFade(Color.fromRGB(nextInt(256), nextInt(256), nextInt(256)))
        .flicker(nextBoolean())
        .trail(nextBoolean())
        .build()

    val firework = this.world.spawn(this, Firework::class.java)
    val fireworkMeta = firework.fireworkMeta.apply {
        addEffect(effect)
        power = nextInt(1, 3)
    }
    firework.fireworkMeta = fireworkMeta
}