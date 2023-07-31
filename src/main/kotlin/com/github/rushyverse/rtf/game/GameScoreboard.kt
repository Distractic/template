package com.github.rushyverse.rtf.game

import com.github.rushyverse.api.APIPlugin.Companion.BUNDLE_API
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.game.team.TeamType
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.RTFPlugin.Companion.BUNDLE_RTF
import com.github.rushyverse.rtf.client.ClientRTF
import java.util.*

object GameScoreboard {

    suspend fun update(
        client: ClientRTF,
        game: Game,
        timeFormatted: String = ""
    ) {
        val locale = client.lang.locale
        val lines = mutableListOf<String>()
        val team = game.getClientTeam(client)
        val state = game.state()
        val stats = client.stats

        lines.add("")
        lines.add(translateStateLine(state, timeFormatted, locale))
        lines.add("")

        if (state == GameState.STARTED) {

            game.teams
                .forEach {
                    lines.add(translateTeamFlagLine(it, locale))
                }

            lines.add("")
        }

        if (team != null) {
            lines.add(translatePlayerTeamLine(locale, team.type))

            if (state == GameState.STARTED) {
                lines.add(translatePlayerKillsLine(locale, stats.kills()))
                lines.add(translatePlayerDeathsLine(locale, stats.deaths()))
                lines.add(translatePlayerAttemptsLine(locale, stats.flagAttempts))
            }
        } else {
            lines.add(translateSpectateLine(locale))
            lines.add(translateJoinUsage(locale, "§E/rtf join"))
        }

        lines.add("")
        lines.add("play.rushy.space")

        client.scoreboard().apply {
            updateTitle("§BRTF #§L${game.id}")
            updateLines(lines)
        }
    }

    private fun translateTeamFlagLine(team: TeamRTF, locale: Locale): String {
        val keyTeamName = "team.${team.type.name.lowercase()}"
        val translateTeamName = RTFPlugin.translationsProvider.translate(keyTeamName, locale, BUNDLE_API)

        val keyFlagState = if (team.flagStolenState) "scoreboard.team.flag.stolen" else "scoreboard.team.flag.placed"
        val translateFlagState = RTFPlugin.translationsProvider.translate(keyFlagState, locale, BUNDLE_RTF)

        return "$translateTeamName: $translateFlagState"
    }

    private fun translateStartedStateLine(locale: Locale, timeFormatted: String) =
        RTFPlugin.translationsProvider.translate("scoreboard.started", locale, BUNDLE_RTF, listOf(timeFormatted))

    private fun translatePlayerKillsLine(locale: Locale, kills: Int): String {
        return RTFPlugin.translationsProvider.translate(
            "scoreboard.kills", locale, BUNDLE_RTF, listOf(
                kills
            )
        )
    }

    private fun translatePlayerDeathsLine(locale: Locale, deaths: Int): String {
        return RTFPlugin.translationsProvider.translate(
            "scoreboard.deaths", locale, BUNDLE_RTF, listOf(
                deaths
            )
        )
    }

    private fun translatePlayerAttemptsLine(locale: Locale, attempts: Int): String {
        return RTFPlugin.translationsProvider.translate(
            "scoreboard.attempts", locale, BUNDLE_RTF, listOf(
                attempts
            )
        )
    }

    private fun translatePlayerTeamLine(locale: Locale, type: TeamType): String {
        return RTFPlugin.translationsProvider.translate(
            "scoreboard.team", locale, BUNDLE_RTF, listOf(
                RTFPlugin.translationsProvider.translate("team.${type.name.lowercase()}", locale, BUNDLE_API)
            )
        )
    }

    private fun translateStateLine(state: GameState, timeFormatted:String, locale: Locale): String {
        val currentState = state.name.lowercase()
        return RTFPlugin.translationsProvider.translate(
            "scoreboard.$currentState", locale, BUNDLE_RTF, listOf(
                when (currentState) {
                    "starting" -> "$timeFormatted"
                    "started" -> "$timeFormatted"
                    "ending" -> "$timeFormatted"
                    else -> ""
                }

            )
        )
    }

    private fun translateSpectateLine(locale: Locale) =
        RTFPlugin.translationsProvider.translate("scoreboard.spectate.mode", locale, BUNDLE_RTF)

    private fun translateJoinUsage(locale: Locale, joinCommand: String) =
        RTFPlugin.translationsProvider.translate("scoreboard.spectate.join.usage", locale, BUNDLE_RTF, listOf(joinCommand))


}