package com.github.rushyverse.rtf

import com.charleskorn.kaml.Yaml
import com.github.rushyverse.api.Plugin
import com.github.rushyverse.api.configuration.reader.IFileReader
import com.github.rushyverse.api.configuration.reader.YamlFileReader
import com.github.rushyverse.api.configuration.reader.readConfigurationFile
import com.github.rushyverse.api.extension.registerListener
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.koin.loadModule
import com.github.rushyverse.api.listener.PlayerListener
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.serializer.LocationSerializer
import com.github.rushyverse.api.translation.ResourceBundleTranslationProvider
import com.github.rushyverse.api.translation.TranslationProvider
import com.github.rushyverse.api.translation.registerResourceBundleForSupportedLocales
import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.commands.RTFCommand
import com.github.rushyverse.rtf.config.MapConfig
import com.github.rushyverse.rtf.config.RTFConfig
import com.github.rushyverse.rtf.game.GameManager
import com.github.rushyverse.rtf.listener.AuthenticationListener
import com.github.rushyverse.rtf.listener.GameListener
import com.github.rushyverse.rtf.listener.UndesirableEventListener
import com.github.shynixn.mccoroutine.bukkit.scope
import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIBukkitConfig
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

        lateinit var translationsProvider: TranslationProvider
            private set
    }


    lateinit var config : RTFConfig private set
    lateinit var configMaps : List<MapConfig> private set

    lateinit var mapsDir: File private set

    lateinit var tempDir: File private set

    override suspend fun onEnableAsync() {
        super.onEnableAsync()
        modulePlugin<RTFPlugin>()

        val configReader = createYamlReader()
        config = configReader.readConfigurationFile<RTFConfig>("config.yml")
        configMaps = configReader.readConfigurationFile<List<MapConfig>>("maps.yml")

        logger.info("Configuration Summary: $config")
        logger.info("Configuration Maps: $configMaps")

        mapsDir = File(dataFolder, "maps").apply { mkdirs() }
        tempDir = setupTempDir()

        translationsProvider = createTranslationProvider()

        loadModule(id) {
            single { GameManager(this@RTFPlugin) }
        }

        RTFCommand(this).register()

        registerListener { AuthenticationListener() }
        registerListener { UndesirableEventListener() }
        registerListener { GameListener() }
    }

    /**
     * Create a new instance of yaml reader.
     * @return The instance of the yaml reader.
     */
    private fun createYamlReader(): IFileReader {
        val yaml = Yaml(
            serializersModule = SerializersModule {
                contextual(LocationSerializer())
            }
        )
        return YamlFileReader(this, yaml)
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
            pluginId = id,
            uuid = player.uniqueId,
            scope = scope + SupervisorJob(scope.coroutineContext.job)
        )
    }

    override suspend fun createTranslationProvider(): ResourceBundleTranslationProvider {
        return (super.createTranslationProvider()).apply {
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
}