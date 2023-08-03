package com.github.rushyverse.rtf.game

import com.github.rushyverse.api.APIPlugin.Companion.BUNDLE_API
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.game.team.TeamType
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.RTFPlugin.Companion.BUNDLE_RTF
import com.github.rushyverse.rtf.client.ClientRTF
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import java.util.*

object GameScoreboard {

    private val emptyLine = text("")
    private val serverIpAddress = text("play.rushy.space")

    suspend fun update(
        client: ClientRTF,
        game: Game,
        timeFormatted: String = ""
    ) {
        val locale = client.lang.locale
        val lines = mutableListOf<Component>()
        val team = game.getClientTeam(client)
        val state = game.state()
        val stats = client.stats

        lines.add(emptyLine)
        lines.add(text(translateStateLine(state, timeFormatted, locale)))
        lines.add(emptyLine)

        if (state == GameState.STARTED) {

            game.teams
                .forEach {
                    lines.add(text(translateTeamFlagLine(it, locale)))
                }

            lines.add(emptyLine)
        }

        if (team != null) {
            lines.add(text(translatePlayerTeamLine(locale, team.type)))

            if (state == GameState.STARTED) {
                lines.add(text(translatePlayerKillsLine(locale, stats.kills())))
                lines.add(text(translatePlayerDeathsLine(locale, stats.deaths())))
                lines.add(text(translatePlayerAttemptsLine(locale, stats.flagAttempts)))
            }
        } else {
            lines.add(text(translateSpectateLine(locale)))
            lines.add(text(translateJoinUsage(locale, "§E/rtf join")))
        }

        lines.add(emptyLine)
        lines.add(serverIpAddress)

        client.scoreboard().apply {
            updateTitle(text("§BRTF #§L${game.id}"))
            updateLines(lines)
        }
    }

    private fun translateTeamFlagLine(team: TeamRTF, locale: Locale): String {
        val keyTeamName = "team.${team.type.name.lowercase()}"
        val translateTeamName = RTFPlugin.translationProvider.translate(keyTeamName, locale, BUNDLE_API)

        val keyFlagState = if (team.flagStolenState) "scoreboard.team.flag.stolen" else "scoreboard.team.flag.placed"
        val translateFlagState = RTFPlugin.translationProvider.translate(keyFlagState, locale, BUNDLE_RTF)

        return "$translateTeamName: $translateFlagState"
    }

    private fun translateStartedStateLine(locale: Locale, timeFormatted: String) =
        RTFPlugin.translationProvider.translate("scoreboard.started", locale, BUNDLE_RTF, timeFormatted)

    private fun translatePlayerKillsLine(locale: Locale, kills: Int): String {
        return RTFPlugin.translationProvider.translate("scoreboard.kills", locale, BUNDLE_RTF, kills)
    }

    private fun translatePlayerDeathsLine(locale: Locale, deaths: Int): String {
        return RTFPlugin.translationProvider.translate("scoreboard.deaths", locale, BUNDLE_RTF, deaths)
    }

    private fun translatePlayerAttemptsLine(locale: Locale, attempts: Int): String {
        return RTFPlugin.translationProvider.translate("scoreboard.attempts", locale, BUNDLE_RTF, attempts)
    }

    private fun translatePlayerTeamLine(locale: Locale, type: TeamType): String {
        val teamNameTranslate =
            RTFPlugin.translationProvider.translate("team.${type.name.lowercase()}", locale, BUNDLE_API)
        return RTFPlugin.translationProvider.translate("scoreboard.team", locale, BUNDLE_RTF, teamNameTranslate)
    }

    private fun translateStateLine(state: GameState, timeFormatted: String, locale: Locale): String {
        val currentState = state.name.lowercase()
        return RTFPlugin.translationProvider.translate(
            "scoreboard.$currentState", locale, BUNDLE_RTF,
            when (currentState) {
                "starting" -> timeFormatted
                "started" -> timeFormatted
                "ending" -> timeFormatted
                else -> ""
            }
        )
    }

    private fun translateSpectateLine(locale: Locale) =
        RTFPlugin.translationProvider.translate("scoreboard.spectate.mode", locale, BUNDLE_RTF)

    private fun translateJoinUsage(locale: Locale, joinCommand: String) =
        RTFPlugin.translationProvider.translate("scoreboard.spectate.join.usage", locale, BUNDLE_RTF, joinCommand)
}