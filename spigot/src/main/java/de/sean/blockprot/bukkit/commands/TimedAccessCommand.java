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

package de.sean.blockprot.bukkit.commands;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.TimedAccessManager;
import de.sean.blockprot.bukkit.util.PlayerNameResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Handles {@code /bp timed <player> <seconds>}.
 *
 * <p>Grants a named player temporary read access to the block the owner is looking at.
 * Access is automatically revoked after the specified number of seconds.
 * All grants are in-memory; a server restart revokes all pending grants.
 *
 * @since 1.2.0
 */
public final class TimedAccessCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS)));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_USAGE)));
            return true;
        }

        final String targetName = args[1];
        final long seconds;
        try {
            seconds = Long.parseLong(args[2]);
            if (seconds <= 0) throw new NumberFormatException("non-positive");
        } catch (NumberFormatException e) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_USAGE)));
            return true;
        }

        Block target = player.getTargetBlockExact(5);
        if (target == null || !BlockProt.getDefaultConfig().isLockable(target.getType())) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_USAGE)));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            OfflinePlayer guest = PlayerNameResolver.findOfflinePlayer(targetName);
            if (guest == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer fallback = Bukkit.getOfflinePlayer(targetName);
                if (fallback.hasPlayedBefore()) guest = fallback;
            }

            if (guest == null || guest.getUniqueId() == null) {
                final String msg = Translator.get(TranslationKey.MESSAGES__TRANSFER_PLAYER_NOT_FOUND)
                    .replace("{player}", targetName);
                Bukkit.getScheduler().runTask(BlockProt.getInstance(), () ->
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg)));
                return;
            }

            final OfflinePlayer finalGuest = guest;
            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                boolean granted = TimedAccessManager.grant(
                    target.getLocation(),
                    finalGuest.getUniqueId(),
                    player.getUniqueId(),
                    seconds
                );

                if (granted) {
                    String name = finalGuest.getName() != null ? finalGuest.getName() : targetName;
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_SUCCESS)
                            .replace("{player}", name)
                            .replace("{seconds}", String.valueOf(seconds))));
                } else {
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_NOT_OWNER)));
                }
            });
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                .toList();
        }
        return Collections.emptyList();
    }
}
