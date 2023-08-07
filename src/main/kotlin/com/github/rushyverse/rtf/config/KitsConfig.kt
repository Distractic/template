package com.github.rushyverse.rtf.config

import com.github.rushyverse.api.serializer.ItemStackSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack

typealias ItemStackSerializable = @Contextual ItemStack

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class KitsConfig(
    val kits: Set<KitConfig>
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class KitConfig(
    val name: String,
    val description: String,
    val icon: ItemStackSerializable,
    val armor: ArmorConfig,
    val items: Set<@Serializable(with = ItemStackSerializer::class) ItemStack>
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class ArmorConfig(
    val helmet: ItemStackSerializable,
    val chestplate: ItemStackSerializable,
    val leggings: ItemStackSerializable,
    val boots: ItemStackSerializable
)
