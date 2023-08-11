package com.github.rushyverse.rtf.ext

/**
 * Extension function to format this value to a formatted time.
 *
 * ## Examples
 *
 * ```kotlin
 * 134.formatSeconds()   // "2m 14s"
 * 1334.formatSeconds()  // "22m 14s"
 * 13334.formatSeconds() // "3h 42m 14s"
 * ```
 */
fun Int.formatSeconds(): String {
    require(this >= 0) { "The number of seconds must be positive." }

    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val secondsRemainder = this % 60

    return when {
        hours > 0 -> "$hours" + "h " + String.format("%02d", minutes) + "m " + String.format(
            "%02d",
            secondsRemainder
        ) + "s"

        minutes > 0 -> "$minutes" + "m " + String.format("%02d", secondsRemainder) + "s"
        else -> "$secondsRemainder" + "s"
    }
}
