package com.github.rushyverse.rtf.config

import com.charleskorn.kaml.Yaml
import com.github.rushyverse.api.configuration.reader.YamlFileReader
import com.github.rushyverse.api.configuration.reader.readConfigurationFile
import com.github.rushyverse.api.game.team.TeamType
import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.serializer.LocationSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bukkit.Location
import org.bukkit.Material
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import org.bukkit.plugin.java.JavaPlugin

class MapConfigListSerializer : KSerializer<List<MapConfig>> {

    private val plugin : JavaPlugin by inject<JavaPlugin>()
    private val mapNameSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = mapNameSerializer.descriptor

    override fun deserialize(decoder: Decoder): List<MapConfig> {
        val mapFileNames = mapNameSerializer.deserialize(decoder)
        val mapConfigs = mutableListOf<MapConfig>()

        for (fileName in mapFileNames) {
            val configReader = YamlFileReader(plugin, Yaml.default)
            val mapConfig = configReader.readConfigurationFile<MapConfig>(fileName)

            mapConfigs.add(mapConfig)
        }

        return mapConfigs
    }

    override fun serialize(encoder: Encoder, value: List<MapConfig>) {
       throw UnsupportedOperationException("Operation not supported.")
    }

}


@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
@SerialName("map")
data class MapConfig(
    val worldTemplateName: String,
    val limitY: Int,
    val allowedBlocks: Set<Material>,
    val teams: List<TeamRTFConfig>
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class TeamRTFConfig(
    val type: TeamType,
    @Serializable(with = LocationSerializer::class)
    val spawnPoint: Location,
    //val spawnCuboid: CubeArea, TODO: serializer not implemented
    @Serializable(with = LocationSerializer::class)
    val flagPoint: Location,
    //val flagCuboid: CubeArea, TODO: serializer not implemented
    val flagMaterial: Material,
)

