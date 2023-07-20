package com.github.rushyverse.rtf.config

import com.github.rushyverse.api.extension.getSectionOrException
import com.github.rushyverse.api.extension.getStringOrException
import com.github.rushyverse.api.game.team.TeamType
import com.github.rushyverse.api.world.Cuboid
import com.github.rushyverse.api.world.Pos
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

data class TeamRTFConfig(
    val type: TeamType,
    val spawnPoint: Pos,
    val spawnCuboid: Cuboid,
    val flagPoint: Pos,
    val flagCuboid: Cuboid,
    val flagMaterial: Material,
) {

    companion object {
        fun parse(section: ConfigurationSection) = TeamRTFConfig(
            TeamType.valueOf(section.name.uppercase()),
            Pos.parse(section.getStringOrException("spawn-point")),
            Cuboid.parse(section.getSectionOrException("spawn-cuboid")),
            Pos.parse(section.getStringOrException("flag-point")),
            Cuboid.parse(section.getSectionOrException("flag-cuboid")),
            Material.getMaterial("${section.name.uppercase()}_WOOL")!!
        )
    }
}