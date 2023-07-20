package com.github.rushyverse.rtf.config

import com.github.rushyverse.api.extension.getSectionOrException
import com.github.rushyverse.api.extension.getStringOrException
import mu.KotlinLogging
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

val logger = KotlinLogging.logger("RTFConfig")

data class RTFConfig(
    val dedicatedServer: Boolean,
    val multiWorld: MultiWorldSection,
    val game: GameSection,
    val rewards: RewardsSection,
    val maps: List<MapConfig>
) {


    companion object {
        fun parse(config: FileConfiguration): RTFConfig {

            val dedicatedServer = config.getBoolean("dedicated-server")
            val multiWorld = config.getSectionOrException("multi-world")
            val game = config.getSectionOrException("game")
            val rewards = config.getSectionOrException("rewards")
            val maps = config.getSectionOrException("maps")

            return RTFConfig(dedicatedServer,
                MultiWorldSection.parse(multiWorld),
                GameSection.parse(game),
                RewardsSection.parse(rewards),
                MapConfig.parseMaps(maps)
            )
        }

    }
}

data class MultiWorldSection(
    val enabled: Boolean,
    val maxWorlds: Int
) {
    companion object {
        fun parse(section: ConfigurationSection): MultiWorldSection {
            return MultiWorldSection(
                section.getBoolean("enabled"),
                section.getInt("max-worlds")
            )
        }
    }
}

data class GameSection(
    val minPlayers: Int,
    val maxPlayers: Int,
    val backToHubCommand: String,

) {
    companion object {
        fun parse(section: ConfigurationSection): GameSection {
            return GameSection(
                section.getInt("min-players"),
                section.getInt("max-players"),
                section.getStringOrException("back-to-hub-command")
            )
        }
    }
}

data class RewardsSection(
    val assist: RewardsInfo,
    val kill: RewardsInfo,
    val win: RewardsInfo,
    val lose: RewardsInfo,
    val vipMultiplier: Double,
) {

    companion object {
        fun parse(section: ConfigurationSection): RewardsSection {
            return RewardsSection(
                RewardsInfo.parse(section.getString("assist")),
                RewardsInfo.parse(section.getString("kill")),
                RewardsInfo.parse(section.getString("win")),
                RewardsInfo.parse(section.getString("lose")),
                section.getDouble("vip-multiplier")
            )
        }

    }
}

enum class RewardType {
    XP,
    COINS
    ;

    companion object {
        fun parse(value: String): RewardType? {
            for (type in values()) {
                if (value.uppercase() == type.name)
                    return type
            }
            return null
        }
    }
}

data class RewardsInfo(
    val rewards: List<RewardInfo>
) {
    companion object {
        fun parse(value: String?): RewardsInfo {
            val rewardsList = mutableListOf<RewardInfo>()

            value?.split(" ")?.forEach {
                rewardsList.add(
                    RewardInfo.parse(it)
                )
            }

            return RewardsInfo(rewardsList)
        }
    }
}

data class RewardInfo(
    val type: RewardType,
    val amount: Int,
) {
    companion object {
        fun parse(value: String): RewardInfo {

            val rewardSplit = value.split(".")
            val amount = rewardSplit[0].toInt()
            val type = RewardType.parse(rewardSplit[1])
                ?: throw IllegalArgumentException("Reward type not found in `$value`")

            return RewardInfo(type, amount)
        }
    }
}