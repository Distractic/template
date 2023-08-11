package com.github.rushyverse.rtf.gui.commons

import com.github.rushyverse.api.koin.inject
import com.github.rushyverse.api.player.Client
import com.github.rushyverse.api.translation.Translator
import com.github.rushyverse.rtf.RTFPlugin
import net.kyori.adventure.text.Component.text
import org.bukkit.Bukkit
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

abstract class GUI(
    val titleKey: String,
    val size: Int,
) {

    private val translator : Translator by inject(RTFPlugin.ID)
    val viewers: MutableList<Client> = mutableListOf()

    suspend fun open(client: Client) {
        val translatedTitle = if (titleKey.contains(".")) {
           translator.get(titleKey, client.lang().locale)
        } else titleKey
        val inv = Bukkit.createInventory(null, size, text(translatedTitle))
        client.requirePlayer().openInventory(inv)
        applyItems(client, inv)
        viewers.add(client)
    }

    fun close(client: Client) {
        viewers.remove(client)
        client.requirePlayer().closeInventory()
    }

    suspend open fun sync() {
        for (viewer in viewers) {
            applyItems(viewer, viewer.requirePlayer().openInventory.topInventory)
        }
    }

    abstract suspend fun applyItems(client: Client, inv: Inventory)

    abstract fun onClick(client: Client, item: ItemStack, clickType: ClickType)
}
