package com.github.rushyverse.rtf.game

import com.github.rushyverse.api.APIPlugin.Companion.BUNDLE_API
import com.github.rushyverse.api.extension.BukkitRunnable
import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.game.GameData
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.schedule.SchedulerTask
import com.github.rushyverse.api.translation.Translator
import com.github.rushyverse.api.translation.getComponent
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.config.MapConfig
import com.github.rushyverse.rtf.config.RTFConfig
import com.github.rushyverse.rtf.ext.formatSeconds
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.scope
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random.Default.nextBoolean
import kotlin.random.Random.Default.nextInt
import kotlin.time.Duration.Companion.seconds

class Game(
    val plugin: RTFPlugin,
    val id: Int,
    val world: World,
    val config: RTFConfig,
    val mapConfig: MapConfig
) {
    val data = GameData("rtf", id)

    val players: Collection<Player>
        get() = world.players

    val gameTask = SchedulerTask(plugin.scope, 1.seconds)

    val teams: List<TeamRTF> = mapConfig.teams.map { TeamRTF(it, world) }

    val createdTime = System.currentTimeMillis()
    var startedTime: Long = 0 // value set when the game starts
    var endTime: Long = 0 // value set when the game ends

    private val clients: ClientManager by inject(plugin.id)
    private val manager: GameManager by inject(plugin.id)

    lateinit var teamWon: TeamRTF private set

    fun state(): GameState = data.state

    fun sendBasicKit(player: Player) {
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

            broadcast("game.message.started", NamedTextColor.GREEN)

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

            gameTask.add { startingTask(this, time) }
            gameTask.run()
        }

        manager.sharedGameData.saveUpdate(data)
    }

    private suspend fun startingTask(task: SchedulerTask.Task, atomicTime: AtomicInteger) {
        val time = atomicTime.get()
        if (time == 0) {
            task.remove() // end the repeating task
            start(true)
            return
        }
        broadcast(
            "game.message.starting",
            NamedTextColor.GREEN,
            argumentBuilder = { arrayOf("$time") }
        )
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
        val locale = client.lang().locale
        client.requirePlayer()
            .let { p ->
                p.gameMode = GameMode.SPECTATOR
                p.inventory.clear()
                p.teleport(world.spawnLocation)

                p.sendMessage(
                    plugin.translator.getComponent(
                        "game.player.spectate",
                        locale,
                        arrayOf("<yellow>/rtf join</yellow>")
                    ).color(NamedTextColor.LIGHT_PURPLE)
                        .hoverEvent(
                            HoverEvent.showText(
                                plugin.translator.getComponent(
                                    "game.player.spectate.hover",
                                    locale
                                )
                            )
                        )
                        .clickEvent(ClickEvent.runCommand("/rtf join"))
                )
            }



        if (startedTime == 0L) // Avoid over-using
            GameScoreboard.update(client, this)

        data.players = players.size
        manager.sharedGameData.saveUpdate(data)
    }

    private fun findJoinTeam(): TeamRTF {
        val minPlayers = teams.minOf { it.members.size }
        val smallestTeams = teams.filter { it.members.size == minPlayers }

        return smallestTeams[nextInt(smallestTeams.size)]
    }

    suspend fun clientJoin(client: ClientRTF) {
        val player = client.requirePlayer()
        val joinedTeam = findJoinTeam().also {
            player.teleport(it.spawnPoint)
            player.gameMode = GameMode.SURVIVAL
            it.members.add(client)

            player.displayName(player.displayName().color(it.type.color))
        }
        val colorName = joinedTeam.type.name.lowercase()

        if (startedTime == 0L)
            GameScoreboard.update(client, this)

        broadcast(
            "player.join.team",
            NamedTextColor.GRAY,
            argumentBuilder = {
                val translatedTeamName = plugin.translator.get("team.$colorName", it, BUNDLE_API).lowercase()
                arrayOf(
                    player.name,
                    "<$colorName>$translatedTeamName</$colorName>"
                )
            }
        )
    }

    suspend fun clientLeave(client: ClientRTF) {
        val playersSize = players.size
        val team = teams.firstOrNull { it.members.contains(client) }

        team?.members?.remove(client)

        data.players = playersSize

        client.player?.apply {
            displayName(displayName().color(NamedTextColor.WHITE))
        }

        // Leave while game is starting and the current number of players is not reached
        if (playersSize < config.game.minPlayers) {

            when (data.state) {
                GameState.STARTING -> {
                    gameTask.tasks[0].remove()

                    broadcast("game.message.client.leave.starting", NamedTextColor.RED)
                    data.state = GameState.WAITING
                }

                GameState.STARTED -> {
                    if (teams.any { it.members.isEmpty() }) {
                        end(null)
                    }
                }

                else -> {}
            }

        }

        manager.sharedGameData.saveUpdate(data)
    }

    suspend fun clientPickupFlag(client: ClientRTF, flagTeam: TeamRTF) {
        val player = client.requirePlayer()

        flagTeam.flagStolenState = true
        client.stats.flagAttempts += 1

        broadcast(
            "player.pickup.flag",
            NamedTextColor.GOLD,
            argumentBuilder = { arrayOf(player.name, flagTeam.type.name) }
        )
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
        val team = getClientTeam(client) ?: return

        client.stats.flagPlaces += 1
        flagTeam.flagStolenState = false

        broadcast(
            "player.place.flag",
            NamedTextColor.GOLD,
            argumentBuilder = { arrayOf(player.name, flagTeam.type.name) }
        )
        client.reward(config.rewards.flagPlace)

        end(team)
    }

    private fun giveWinRewards(winTeam: TeamRTF) {
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
            broadcast("game.end.other", NamedTextColor.RED)
            ejectPlayersAndDestroy()
        } else {
            this.endTime = System.currentTimeMillis()
            this.teamWon = winTeam
            broadcastWinMessage(winTeam)
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

    private suspend fun broadcastWinMessage(
        winTeam: TeamRTF,
        bestPlayers: Set<String> = setOf("Carlito", "Mcflush", "Poublito")
    ) {
        val separator = "<rainbow>--------------------------------".asComponent()
        val void = Component.empty()
        val teamColor = winTeam.type.name

        fun broadcastSeparator() =
            world.players.forEach { it.sendMessage(separator) }

        fun broadcastVoid() =
            world.players.forEach { it.sendMessage(void) }

        broadcastSeparator()
        broadcast(
            "game.end.win",
            NamedTextColor.LIGHT_PURPLE,
            argumentBuilder = {
                val teamName = winTeam.type.name(this, it)
                    .lowercase()
                arrayOf("<$teamColor>$teamName</$teamColor>")
            }
        )
        broadcast("game.end.time",
            NamedTextColor.GRAY,
            argumentBuilder = {
                val elapsedTime = ((endTime - startedTime) / 1000)
                    .toInt()
                    .formatSeconds()
                arrayOf("<yellow>$elapsedTime</yellow>")
            }
        )
        broadcastVoid()
        broadcast("game.end.top", NamedTextColor.GRAY)
        bestPlayers.forEachIndexed { index, player ->
            val topColor = when (index) {
                0 -> NamedTextColor.LIGHT_PURPLE
                1 -> NamedTextColor.GOLD
                else -> NamedTextColor.YELLOW
            }
            broadcast("game.end.top.${index + 1}",
                topColor,
                argumentBuilder = {
                    arrayOf(player)
                }
            )
        }
        broadcastVoid()
        broadcastSeparator()
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

    /**
     * Broadcast a translated message using the game world.
     */
    suspend fun broadcast(
        key: String,
        color: NamedTextColor = NamedTextColor.WHITE,
        argumentBuilder: Translator.(Locale) -> Array<Any> = { emptyArray() }
    ) = plugin.broadcast(world.players, key, argumentBuilder = argumentBuilder, messageModifier =  { it.color(color) })

    fun isProtectedLocation(location: Location): Boolean {
        return teams.any { it.spawnCuboid.isInArea(location) || it.flagCuboid.isInArea(location) }
    }

    fun isBlockAllowed(blockType: Material): Boolean {
        return mapConfig.allowedBlocks.contains(blockType)
    }
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
