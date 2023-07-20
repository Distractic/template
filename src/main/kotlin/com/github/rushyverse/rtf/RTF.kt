package com.github.rushyverse.rtf

import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.commands.RTFCommand
import com.github.rushyverse.rtf.config.RTFConfig
import com.github.rushyverse.rtf.listener.*
import com.github.rushyverse.rtf.manager.GameManager
import com.github.shynixn.mccoroutine.bukkit.scope
import com.github.rushyverse.api.Plugin
import com.github.rushyverse.api.extension.registerListener
import com.github.rushyverse.api.game.GameData
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.*
import com.github.rushyverse.api.translation.*
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File
import java.util.*

class RTF(
    override val id: String = "RTFPlugin",
) : Plugin() {

    companion object {
        const val BUNDLE_RTF = "rtf_translate"

        lateinit var translationsProvider: TranslationsProvider
            private set
    }


    override val clientEvents: PluginClientEvents = RTFClientEvents(this)

    lateinit var config : RTFConfig private set

    lateinit var mapsDir: File private set

    lateinit var tempDir: File private set

    lateinit var gameManager: GameManager private set


    override suspend fun onEnableAsync() {
        super.onEnableAsync()

        config = RTFConfig.parse(getConfig())
        saveDefaultConfig()
        logger.info("Configuration Summary: $config")

        mapsDir = File(dataFolder, "maps").apply { mkdirs() }
        tempDir = setupTempDir()

        translationsProvider = createTranslationsProvider()

        gameManager = GameManager(this)

        RTFCommand(this).register()

        modulePlugin<RTF>()
        moduleClients()

        registerListener { UndesirableEventListener() }
        registerListener { GameListener(this) }
    }

    private fun setupTempDir() = File(dataFolder, "temp").apply {
        if (exists()) {
            // Clear the directory content
            this.listFiles()?.forEach {
                it.deleteRecursively()
            }
        } else {
            mkdirs()
        }
    }

    override suspend fun onDisableAsync() {
        super.onDisableAsync()
    }

    override fun createClient(player: Player): Client {
        return ClientRTF(
            pluginId = id,
            uuid = player.uniqueId,
            scope = scope + SupervisorJob(scope.coroutineContext.job)
        )
    }

    override suspend fun createTranslationsProvider(): ResourceBundleTranslationsProvider {
        return (super.createTranslationsProvider()).apply {
            registerResourceBundleForSupportedLocales(BUNDLE_RTF, ResourceBundle::getBundle)
        }
    }

    suspend fun broadcast(world: World, key: String, args: Collection<Any>) {
        val clients: ClientManager by inject(id)

        val clientsByLang = world.players.groupBy {
            clients.getClient(it).lang.locale
        }

        for ((lang, players) in clientsByLang) {
            val translatedMsg = translationsProvider.translate(key, lang, BUNDLE_RTF, args)

            players.forEach { player ->
                val client = clients.getClient(player)
                client.send(translatedMsg)
            }
        }
    }

    fun saveUpdate(data: GameData) {
        gameManager.sharedGameData.saveUpdate(data)
    }

}