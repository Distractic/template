package com.github.rushyverse.rtf.manager

import com.github.rushyverse.rtf.RTF
import com.github.rushyverse.api.SharedMemory
import com.github.rushyverse.api.game.SharedGameData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.WorldType
import java.io.File
import kotlin.coroutines.CoroutineContext

class GameManager(val plugin: RTF) {

    val sharedGameData = SharedMemory.games

    val games: MutableList<Game> = mutableListOf()

    fun getByWorld(world: World): Game? {
        games.forEach {
            if (it.world == world) return it
        }

        return null
    }

    fun findNewGameIndex(): Int {
        return games.size + 1
    }

    suspend fun createAndSave(gameIndex: Int = findNewGameIndex()): Game {
        val worldName = "rtf$gameIndex"
        var world = plugin.server.getWorld(worldName)

        if (world == null) {
            world = createWorldFromTemplate(worldName)
            return Game(plugin, gameIndex, world, plugin.config, plugin.config.maps[0]).also {
                games.add(it)

                sharedGameData.saveUpdate(it.data)
            }
        } else {
            throw IllegalStateException("A game already exists for this world $worldName")
        }
    }

    /**
     * Should be executed in [Dispatcher.IO] coroutine context.
     */
    private suspend fun createWorldFromTemplate(
        worldName: String,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ): World {
        val templateWorld = File(plugin.mapsDir, "classic")
        val target = File(plugin.tempDir, worldName)
        val correctPath = target.path.replace("\\", "/")
        val creator = WorldCreator(correctPath)
        withContext(coroutineContext) {
            templateWorld.copyRecursively(target, true)
            creator.apply {
                type(WorldType.FLAT)
                environment(World.Environment.NORMAL)
                generateStructures(false)
            }
        }

        return plugin.server.createWorld(creator) ?: throw IllegalStateException("Can't create world for $worldName")
    }

    /**
     * Unloads and deletes the world of the given game.
     * After the file deletion, game is removed from the list
     * and from [SharedGameData].
     * This method also calls [SharedGameData.callOnChange] to update related games services.
     *
     * May not work properly if players are still present in the world.
     */
    suspend fun removeGameAndDeleteWorld(
        game: Game,
        coroutineContext: CoroutineContext = Dispatchers.IO
    ) {
        val world = game.world
        val file = world.worldFolder

        Bukkit.unloadWorld(world, false)

        withContext(coroutineContext) {
            file.deleteRecursively()
        }

        sharedGameData.apply {
            games.removeIf { it.id == game.id }
            callOnChange()
        }

        games.remove(game)
    }


    fun getGame(gameIndex: Int) = games.firstOrNull { it.id == gameIndex }
}