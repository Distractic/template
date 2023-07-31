package com.github.rushyverse.rtf.game

import com.github.rushyverse.rtf.client.ClientRTF
import com.github.rushyverse.rtf.config.TeamRTFConfig
import org.bukkit.World

class TeamRTF(
    val config: TeamRTFConfig,
    val world: World
) {

    val type get() = config.type
    val spawnPoint = config.spawnPoint.also { it.world = world }
    val spawnCuboid = config.spawnCuboid.also {
        it.min.world = world
        it.max.world = world
    }
    val flagPoint = config.flagPoint.also { it.world = world }
    val flagCuboid = config.flagCuboid.also {
        it.min.world = world
        it.max.world = world
    }

    val flagMaterial = config.flagMaterial

    val members = mutableListOf<ClientRTF>()
    var flagStolenState = false
        set(value) {
            field = value
            if (!value) {
                flagPoint.block.type = flagMaterial
            }
        }
}