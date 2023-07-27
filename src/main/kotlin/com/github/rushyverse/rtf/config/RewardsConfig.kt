package com.github.rushyverse.rtf.config

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class RewardsConfig(
    val vipMultiplier: Int,
    val assist: Int,
    val kill: Int,
    val win: Int,
    val lose: Int,
)

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Reward(
    val xp: Int,
    val coins: Int,
)