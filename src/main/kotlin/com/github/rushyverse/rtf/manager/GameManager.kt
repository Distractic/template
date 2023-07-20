package com.github.rushyverse.rtf.manager

import com.github.rushyverse.rtf.RTF
import com.github.rushyverse.api.SharedMemory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    fun getGame(gameIndex: Int) = games.firstOrNull { it.id == gameIndex }
}