package com.github.rushyverse.rtf.listener

import com.github.rushyverse.api.extension.event.cancelIf
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.weather.WeatherChangeEvent

class UndesirableEventListener : Listener {

    private fun isRTFWorld(world: World) = world.name.contains("rtf")

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onWeatherChange(event: WeatherChangeEvent) {
        if (event.toWeatherState())
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) = event.cancelIf { isRTFWorld(event.player.world) }
}