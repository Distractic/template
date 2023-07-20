package com.github.rushyverse.rtf.client

import com.github.rushyverse.api.game.stats.KillableStats
import com.github.rushyverse.api.game.stats.Stats
import com.github.rushyverse.api.game.stats.WinnableStats


data class RTFStats(
    val killableStats: KillableStats = KillableStats(),
    var flagAttempts: Int = 0,
    var flagPlaces: Int = 0,
    val winnableStats: WinnableStats = WinnableStats(),
) : Stats {

    override fun calculateScore(): Int {
        val score = killableStats.calculateScore() + winnableStats.calculateScore() + flagAttempts + (flagPlaces * 5)
        if (score < 0)
            return 0
        return score
    }

    fun kills() = killableStats.kills
    fun incKills() { killableStats.kills = killableStats.kills.inc() }

    fun deaths() = killableStats.deaths
    fun incDeaths() { killableStats.deaths = killableStats.deaths.inc() }

    fun wins() = winnableStats.wins
    fun incWins() { winnableStats.wins = winnableStats.wins.inc() }

    fun loses() = winnableStats.loses
    fun incLoses() { winnableStats.loses = winnableStats.loses.inc() }
}
