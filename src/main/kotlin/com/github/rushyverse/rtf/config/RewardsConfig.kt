package com.github.rushyverse.rtf.config

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class RewardsConfig(
    val vipMultiplier: Reward,
    val kill: Reward,
    val assist: Reward,
    val flagPickUp: Reward,
    val flagPlace: Reward,
    val win: Reward,
    val lose: Reward,
) {

}

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class Reward(
    val xp: Int,
    val coins: Int,
)