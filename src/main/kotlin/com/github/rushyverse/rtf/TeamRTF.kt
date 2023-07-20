package com.github.rushyverse.rtf

import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.config.TeamRTFConfig
import org.bukkit.World

class TeamRTF(
    val config: TeamRTFConfig,
    val world: World
) {

    val type = config.type
    val spawnPoint = config.spawnPoint.toLocation(world)
    val spawnCuboid = config.spawnCuboid
    val flagPoint = config.flagPoint.toLocation(world)
    val flagCuboid = config.flagCuboid
    val flagMaterial = config.flagMaterial

    val members = mutableListOf<ClientRTF>()
    var flagStolenState = false
}