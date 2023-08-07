package com.github.rushyverse.rtf

import com.charleskorn.kaml.Yaml
import com.github.rushyverse.api.Plugin
import com.github.rushyverse.api.configuration.reader.IFileReader
import com.github.rushyverse.api.configuration.reader.YamlFileReader
import com.github.rushyverse.api.configuration.reader.readConfigurationFile
import com.github.rushyverse.api.extension.registerListener
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.koin.loadModule
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.serializer.LocationSerializer
import com.github.rushyverse.api.translation.ResourceBundleTranslationProvider
import com.github.rushyverse.api.translation.TranslationProvider
import com.github.rushyverse.api.translation.registerResourceBundleForSupportedLocales
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.commands.RTFCommand
import com.github.rushyverse.rtf.config.*
import com.github.rushyverse.rtf.game.GameManager
import com.github.rushyverse.rtf.gui.KitsGUI
import com.github.rushyverse.rtf.listener.*
import com.github.shynixn.mccoroutine.bukkit.scope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.plus
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.bukkit.World
import org.bukkit.entity.Player
import java.io.File
import java.util.*

class RTFPlugin(
    override val id: String = ID,
) : Plugin() {

    companion object {
        const val BUNDLE_RTF = "rtf_translate"
        const val ID = "RTF"

        lateinit var translationProvider: TranslationProvider
            private set
    }


    lateinit var config: RTFConfig private set
    lateinit var configMaps: List<MapConfig> private set
    lateinit var configKits: KitsConfig private set

    lateinit var mapsDir: File private set

    lateinit var tempDir: File private set

    lateinit var kitsGui: KitsGUI private set

    override suspend fun onEnableAsync() {
        super.onEnableAsync()
        modulePlugin<RTFPlugin>()

        loadConfiguration()

        mapsDir = File(dataFolder, "maps").apply { mkdirs() }
        tempDir = setupTempDir()

        translationProvider = createTranslationProvider()

        loadModule(id) {
            single { GameManager(this@RTFPlugin) }
        }

        kitsGui = KitsGUI(configKits)

        RTFCommand(this).register()

        registerListener { GUIListener(this, setOf(kitsGui)) }
        registerListener { AuthenticationListener() }
        registerListener { UndesirableEventListener() }
        registerListener { GameListener() }
    }

    private fun loadConfiguration() {
        val configReader = createYamlReader()
        config = configReader.readConfigurationFile<RTFConfig>("config.yml")
        configMaps = configReader.readConfigurationFile<List<MapConfig>>("maps.yml")
        configKits = configReader.readConfigurationFile<KitsConfig>("kits.yml")

        logger.info("Configuration Summary: $config")
        logger.info("Configuration Maps: $configMaps")
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

    override fun createClient(player: Player): Client {
        return ClientRTF(
            uuid = player.uniqueId,
            scope = scope + SupervisorJob(scope.coroutineContext.job)
        )
    }

    override suspend fun createTranslationProvider(): ResourceBundleTranslationProvider {
        return (super.createTranslationProvider()).apply {
            registerResourceBundleForSupportedLocales(BUNDLE_RTF, ResourceBundle::getBundle)
        }
    }

    suspend fun broadcast(world: World, key: String, args: Array<Any>) {
        val clients: ClientManager by inject(id)

        val clientsByLang = world.players.groupBy {
            clients.getClient(it).lang.locale
        }

        for ((lang, players) in clientsByLang) {
            val translatedMsg = translationProvider.translate(key, lang, BUNDLE_RTF, args)

            players.forEach { player ->
                val client = clients.getClient(player)
                client.send(translatedMsg)
            }
        }
    }
}