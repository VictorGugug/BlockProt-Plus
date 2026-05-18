/*
 * Copyright (C) 2021 - 2025 spnda
 * This file is part of BlockProt <https://github.com/spnda/BlockProt>.
 *
 * BlockProt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BlockProt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BlockProt.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.VersionCompat;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Chat-based text input. Closes the player's inventory, shows a prompt in chat,
 * provides tab-completion for online player names (when available), and calls
 * onConfirm on the main thread. Typing the cancel word aborts silently.
 *
 * <p>Tab-completion uses {@code com.destroystokyo.paper.event.server.AsyncTabCompleteEvent}
 * when available (Paper 1.21.x). On Paper 26.x+ that class was removed; tab-completion
 * is silently skipped and the rest of ChatInput still works normally.
 *
 * <p>Requires Paper. On plain Spigot servers the anvil-based fallback
 * ({@link AnvilInput}) is used instead.
 */
public final class ChatInput implements Listener {

    /** True if the legacy AsyncTabCompleteEvent class is available at runtime. */
    private static final boolean HAS_LEGACY_TAB_COMPLETE;
    static {
        boolean found = false;
        try {
            Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
            found = true;
        } catch (ClassNotFoundException ignored) {}
        HAS_LEGACY_TAB_COMPLETE = found;
    }

    private final UUID playerUuid;
    private final Plugin plugin;
    private final @Nullable Consumer<String> onConfirm;
    private final String cancelWord;
    private boolean consumed = false;

    private ChatInput(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @Nullable Consumer<String> onConfirm
    ) {
        this.playerUuid = player.getUniqueId();
        this.plugin = plugin;
        this.onConfirm = onConfirm;
        this.cancelWord = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_CANCEL_WORD).trim().toLowerCase();

        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.closeInventory();

        String prompt = Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_PROMPT)
            .replace("{cancel}", cancelWord);
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(prompt));
    }

    public static void open(
            @NotNull Player player,
            @NotNull Plugin plugin,
            @Nullable Consumer<String> onConfirm
    ) {
        new ChatInput(player, plugin, onConfirm);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(playerUuid)) return;
        event.setCancelled(true);

        final String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (text.equalsIgnoreCase(cancelWord)) {
            event.getPlayer().sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__CHAT_INPUT_CANCELLED)
            ));
            unregister();
            return;
        }

        unregister();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (onConfirm != null) onConfirm.accept(text);
        });
    }

    /**
     * Tab-complete handler — only registered/active if the legacy class exists.
     * On Paper 26.x this method never fires because the event class is absent,
     * so no ClassNotFoundException is thrown at runtime.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onTabComplete(org.bukkit.event.Cancellable event) {
        if (!HAS_LEGACY_TAB_COMPLETE) return;
        try {
            var tabEvent = (com.destroystokyo.paper.event.server.AsyncTabCompleteEvent) event;
            if (!(tabEvent.getSender() instanceof Player player)) return;
            if (!player.getUniqueId().equals(playerUuid)) return;
            if (tabEvent.isCommand()) return;

            final String buffer = tabEvent.getBuffer();
            final List<String> names = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(buffer.toLowerCase()))
                .sorted()
                .collect(Collectors.toList());

            if (!names.isEmpty()) {
                tabEvent.setCompletions(names);
                tabEvent.setHandled(true);
            }
        } catch (ClassCastException ignored) {}
    }

    private void unregister() {
        if (!consumed) {
            consumed = true;
            HandlerList.unregisterAll(this);
        }
    }
}
