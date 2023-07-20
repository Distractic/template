package com.github.rushyverse.rtf.config

import com.github.rushyverse.api.extension.getSectionOrException
import com.github.rushyverse.api.game.team.TeamType
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

data class MapConfig(
    val limitY: Int,
    val allowedBlocks: Set<Material>,
    val teams: List<TeamRTFConfig>
) {
    companion object {
        fun parse(section: ConfigurationSection) = MapConfig(
            section.getInt("limit-y"),
            parseMaterials(section.getStringList("allowed-blocks")),
            parseTeamsList(section.getSectionOrException("teams"))
        )

        private fun parseMaterials(list: List<String>): Set<Material> {
            val mutableSet = mutableSetOf<Material>()
            for (blockTypeName in list) {
                mutableSet.add(Material.valueOf(blockTypeName))
            }
            return mutableSet
        }

        private fun parseTeamsList(section: ConfigurationSection): List<TeamRTFConfig> {
            val teamsList = mutableListOf<TeamRTFConfig>()

            for (supportedTeamType in TeamType.values()) {
                val teamName = supportedTeamType.name.lowercase()
                val teamSection = section.getConfigurationSection(teamName) ?: continue
                teamsList.add(TeamRTFConfig.parse(teamSection))
            }

            return teamsList
        }

        fun parseMaps(section: ConfigurationSection): List<MapConfig> {
            val listOfMaps = mutableListOf<MapConfig>()
            for (map in section.getKeys(false)){
                listOfMaps.add(parse(section.getSectionOrException(map)))
            }
            return listOfMaps
        }

    }
}