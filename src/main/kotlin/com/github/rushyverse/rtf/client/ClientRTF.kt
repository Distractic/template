package com.github.rushyverse.rtf.client

import com.github.rushyverse.api.player.Client
import kotlinx.coroutines.CoroutineScope
import java.util.*

class ClientRTF(
    val stats: RTFStats = RTFStats(),
    pluginId: String,
    uuid: UUID,
    scope: CoroutineScope
) : Client(pluginId, uuid, scope) {

}