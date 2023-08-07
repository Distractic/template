package com.github.rushyverse.rtf.config

import kotlinx.serialization.Serializable
import mu.KotlinLogging

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class RTFConfig(
    val gameServer: Boolean, // TODO: not implemented
    val dataProvider: DataProviderConfig,
    val game: GameConfig,
    val rewards: RewardsConfig
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class DataProviderConfig(
    val apiSharedMemory: Boolean
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class GameConfig(
    val maxGames: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val backToHubCommand: String
)
