package com.github.rushyverse.rtf.config

import com.github.rushyverse.api.serializer.ItemStackSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

typealias ItemStackSerializable = @Contextual ItemStack

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class KitsConfig(
    val kits: Set<Kit>
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Kit(
    val name: String,
    val description: String,
    val icon: ItemStackSerializable,
    val armor: ArmorConfig,
    val items: Set<@Serializable(with = ItemStackSerializer::class) ItemStack>
) {

    fun sendItems(inventory: PlayerInventory) {
        armor.let {
            inventory.helmet = it.helmet
            inventory.chestplate = it.chestplate
            inventory.leggings = it.leggings
            inventory.boots = it.boots
        }

        inventory.addItem(*items.toTypedArray())
    }
}

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class ArmorConfig(
    val helmet: ItemStackSerializable,
    val chestplate: ItemStackSerializable,
    val leggings: ItemStackSerializable,
    val boots: ItemStackSerializable
)
