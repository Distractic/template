package com.github.rushyverse.rtf

import com.github.rushyverse.api.Plugin
import com.github.rushyverse.api.configuration.reader.readConfigurationFile
import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.extension.registerListener
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.koin.loadModule
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.player.ClientManager
import com.github.rushyverse.api.translation.ResourceBundleTranslator
import com.github.rushyverse.api.translation.Translator
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
import net.kyori.adventure.text.format.NamedTextColor
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

        lateinit var translator: Translator private set
    }


    lateinit var config: RTFConfig private set
    lateinit var configMaps: List<MapConfig> private set
    lateinit var configKits: KitsConfig private set

    lateinit var mapsDir: File private set

    lateinit var tempDir: File private set

    lateinit var kitsGui: KitsGUI private set

    private val clientManager: ClientManager by inject(id)

    override suspend fun onEnableAsync() {
        super.onEnableAsync()
        modulePlugin<RTFPlugin>()

        loadConfiguration()

        mapsDir = File(dataFolder, "maps").apply { mkdirs() }
        tempDir = setupTempDir()

        translator = createTranslator()

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

    override suspend fun createTranslator(): ResourceBundleTranslator =
        super.createTranslator().apply {
            registerResourceBundleForSupportedLocales(BUNDLE_RTF, ResourceBundle::getBundle)
        }

    /**
     * Broadcasts a localized message to all players in the specified world.
     *
     * This function groups players by their language preferences, translates the message once per language,
     * and then sends the appropriate localized message to each player.
     *
     * @param world The world where the players to receive the message are located.
     * @param key The key used to look up the translation in the resource bundle.
     * @param color The color in which the message should be displayed (default is white).
     * @param args The arguments to format the translated string if it has placeholders.
     */
    suspend fun broadcast(
        world: World,
        key: String,
        color: NamedTextColor = NamedTextColor.WHITE,
        args: Array<Any>
    ) {
        val clientsByLang = world.players.groupBy { clientManager.getClient(it).lang.locale }

        for ((lang, players) in clientsByLang) {
            val translatedMsg = translator.translate(key, lang, BUNDLE_RTF, args)
                .asComponent().color(color)

            players.forEach { player ->
                val client = clientManager.getClient(player)
                client.send(translatedMsg)
            }
        }
    }
}
