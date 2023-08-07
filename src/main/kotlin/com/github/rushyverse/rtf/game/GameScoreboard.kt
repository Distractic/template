package com.github.rushyverse.rtf.game

import com.github.rushyverse.api.extension.asComponent
import com.github.rushyverse.api.extension.withBold
import com.github.rushyverse.api.game.GameState
import com.github.rushyverse.api.game.team.TeamType
import com.github.rushyverse.rtf.RTFPlugin
import com.github.rushyverse.rtf.RTFPlugin.Companion.BUNDLE_RTF
import com.github.rushyverse.rtf.client.ClientRTF
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import java.util.*

object GameScoreboard {

    private val emptyLine = Component.empty()
    private val scoreboardTitle = "<gradient:yellow:gold:red>RushTheFlag"
        .asComponent().withBold()
    private val serverIpAddress = "<gradient:gold:light_purple:dark_purple:red>play.rushy.space"
        .asComponent().withBold()

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
        lines.add(translateStateLine(timeFormatted, game, locale))
        lines.add(emptyLine)

        if (state == GameState.STARTED) {

            game.teams
                .forEach {
                    lines.add(translateTeamFlagLine(it, locale))
                }

            lines.add(emptyLine)
        }

        if (team != null) {
            lines.add(translateTeamLine(locale, team.type))

            if (state == GameState.STARTED) {
                lines.add(translateKillsLine(locale, stats.kills()))
                lines.add(translateDeathsLine(locale, stats.deaths()))
                lines.add(translateAttemptsLine(locale, stats.flagAttempts))
            }
        } else {
            lines.add(translateSpectateLine(locale))
            lines.add(translateJoinUsage(locale, "<yellow>/rtf join"))
        }

        lines.add(emptyLine)
        lines.add(serverIpAddress)

        client.scoreboard().apply {
            updateTitle(scoreboardTitle)
            updateLines(lines)
        }
    }

    private fun translateTeamFlagLine(team: TeamRTF, locale: Locale): Component {
        val color = team.type.color
        val keyFlagState = if (team.flagStolenState) "scoreboard.team.flag.stolen" else "scoreboard.team.flag.placed"
        val translateFlagState = RTFPlugin.translator.translate(keyFlagState, locale, BUNDLE_RTF)
        val flagStateColor = if (team.flagStolenState)
            NamedTextColor.GOLD else NamedTextColor.GREEN

        return text(team.type.name(RTFPlugin.translator), color)
            .append(text(": ", NamedTextColor.GRAY))
            .append(text(translateFlagState, flagStateColor))
    }

    private fun translateKillsLine(locale: Locale, kills: Int): Component {
        return RTFPlugin.translator.translate(
            "scoreboard.kills", locale, BUNDLE_RTF,
            arrayOf("<green>$kills")
        ).asComponent()
    }

    private fun translateDeathsLine(locale: Locale, deaths: Int): Component {
        return RTFPlugin.translator.translate("scoreboard.deaths", locale, BUNDLE_RTF, arrayOf("<red>$deaths"))
            .asComponent()
    }

    private fun translateAttemptsLine(locale: Locale, attempts: Int): Component {
        return RTFPlugin.translator.translate(
            "scoreboard.attempts",
            locale,
            BUNDLE_RTF,
            arrayOf("<light_purple>$attempts")
        ).asComponent()
    }

    private fun translateTeamLine(locale: Locale, type: TeamType): Component {
        val translatedName = type.name(RTFPlugin.translator, locale)
        val color = type.name.lowercase()
        val line = RTFPlugin.translator.translate(
            "scoreboard.team", locale, BUNDLE_RTF,
            arrayOf("<$color>$translatedName</$color>")
        )
        return line.asComponent()
    }

    private fun translateStateLine(timeFormatted: String, game: Game, locale: Locale) =
        when (game.state()) {
            GameState.WAITING -> translateLine("scoreboard.waiting", locale).color(NamedTextColor.GRAY)
            GameState.STARTING -> translateLine("scoreboard.starting", locale, arrayOf(timeFormatted))
            GameState.STARTED -> translateLine("scoreboard.started", locale, arrayOf("<aqua>$timeFormatted"))
            GameState.ENDING -> translateStateEndLine(game, locale)
        }

    private fun translateLine(key: String, locale: Locale, args: Array<Any> = emptyArray()) =
        RTFPlugin.translator.let { translator ->
            translator.translate(
                key, locale, BUNDLE_RTF, args
            ).asComponent()
        }

    private fun translateStateEndLine(game: Game, locale: Locale) = RTFPlugin.translator.let { translator ->
        val color = game.teamWon.type.name.lowercase()
        val teamName = game.teamWon.type.name(translator, locale)
        val translatedLine = translator.translate(
            "scoreboard.ending", locale, BUNDLE_RTF,
            arrayOf("<$color>$teamName</$color>")
        )
        text(translatedLine)
    }

    private fun translateSpectateLine(locale: Locale) = text(
        RTFPlugin.translator.translate("scoreboard.spectate.mode", locale, BUNDLE_RTF),
        NamedTextColor.LIGHT_PURPLE
    ).withBold()

    private fun translateJoinUsage(locale: Locale, joinCommandUsage: String) =
        RTFPlugin.translator.translate(
            "scoreboard.spectate.join.usage",
            locale,
            BUNDLE_RTF,
            arrayOf(joinCommandUsage)
        ).asComponent().color(NamedTextColor.LIGHT_PURPLE)
}
