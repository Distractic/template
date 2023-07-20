package com.github.rushyverse.rtf.commands

import com.github.rushyverse.rtf.RTF
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.shynixn.mccoroutine.bukkit.launch
import dev.jorel.commandapi.arguments.IntegerArgument
import dev.jorel.commandapi.kotlindsl.*
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.ClientManager

class RTFCommand(
    val plugin: RTF
) {

    /**
     * rtf - spectate <id>
     * rtf - join - only in game
     * rtf - leave - only in game
     * rtf - point - <teamType> - only in game for master dev
     */
    suspend fun register() {
        val clients: ClientManager by inject(plugin.id);
        val manager = plugin.gameManager

        commandAPICommand("rtf") {

            subcommand("spectate") {

                withArguments(IntegerArgument("game"))

                playerExecutor { player, args ->
                    val gameIndex = args[0] as Int
                    var game = manager.getGame(gameIndex)

                    plugin.launch {

                        if (game == null && gameIndex == 1) {
                            game = manager.createAndSave(gameIndex)
                        }

                        game?.clientSpectate(clients.getClient(player) as ClientRTF)
                    }
                }
            }

            subcommand("join") {
                playerExecutor { player, _ ->
                    val game = manager.getByWorld(player.world) ?: return@playerExecutor

                    plugin.launch {
                        val client = clients.getClient(player) as ClientRTF

                        if (game.getClientTeam(client) != null) {
                            client.send("join.already.in.team")
                            return@launch
                        }

                        game.clientJoin(client)
                    }
                }
            }

            /*subcommand("leave") {
                playerExecutor { player, _ ->
                    val game = manager.getByWorld(player.world) ?: return@playerExecutor

                    plugin.launch {

                        player.performCommand(plugin.config.game.backToHubCommand)
                    }
                }
            }*/

            subcommand("start"){
                playerExecutor { player, _ ->
                    val game = manager.getByWorld(player.world) ?: return@playerExecutor

                    if (game.state() != GameState.STARTED){
                        plugin.launch { game.start(true) }
                    } else {
                        player.sendMessage("The game is already started.")
                    }
                }
            }

            subcommand("point") {
                stringArgument("team") {
                    playerExecutor { player, args ->
                        val game = manager.getByWorld(player.world) ?: return@playerExecutor
                        val team = game.teams.firstOrNull { it.type.name.equals(args[0] as String, true) }
                        plugin.launch {
                            val client = clients.getClient(player) as ClientRTF

                            if (team == null) {
                                client.send("team.not.found")
                                return@launch
                            }

                            // TODO
                        }

                    }
                }
            }

            subcommand("create") {
                anyExecutor { sender, _ ->
                    plugin.launch {
                        val game = manager.createAndSave()
                        sender.sendMessage("Created game rtf #${game.id}")
                    }
                }
            }
        }
    }
}