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
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.StatHandler;
import de.sean.blockprot.bukkit.nbt.stats.PlayerBlocksStatistic;
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
 * Handles {@code /bp transfer <player>}.
 *
 * <p>The player must be looking at a block they own. Ownership is transferred to
 * {@code <player>}; the old owner is added as a regular friend so they keep access.
 *
 * @since 1.2.0
 */
public final class TransferCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__ONLY_PLAYERS)));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_USAGE)));
            return true;
        }

        final String targetName = args[1];

        // The block the player is looking at.
        Block target = player.getTargetBlockExact(5);
        if (target == null || !BlockProt.getDefaultConfig().isLockable(target.getType())) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                Translator.get(TranslationKey.MESSAGES__TRANSFER_USAGE)));
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(), () -> {
            OfflinePlayer newOwner = PlayerNameResolver.findOfflinePlayer(targetName);
            if (newOwner == null) {
                @SuppressWarnings("deprecation")
                OfflinePlayer fallback = Bukkit.getOfflinePlayer(targetName);
                if (fallback.hasPlayedBefore()) newOwner = fallback;
            }

            if (newOwner == null || newOwner.getUniqueId() == null) {
                final String msg = Translator.get(TranslationKey.MESSAGES__TRANSFER_PLAYER_NOT_FOUND)
                    .replace("{player}", targetName);
                Bukkit.getScheduler().runTask(BlockProt.getInstance(), () ->
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(msg)));
                return;
            }

            final OfflinePlayer finalNewOwner = newOwner;

            Bukkit.getScheduler().runTask(BlockProt.getInstance(), () -> {
                if (finalNewOwner.getUniqueId().equals(player.getUniqueId())) {
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TRANSFER_SELF)));
                    return;
                }

                BlockNBTHandler handler;
                try {
                    handler = new BlockNBTHandler(target);
                } catch (RuntimeException e) {
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TRANSFER_USAGE)));
                    return;
                }

                if (!handler.isOwner(player.getUniqueId())) {
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TRANSFER_NOT_OWNER)));
                    return;
                }

                var result = handler.transferOwner(
                    player.getUniqueId().toString(),
                    finalNewOwner.getUniqueId().toString()
                );

                if (result.success) {
                    // Update statistics: move block from old owner to new owner.
                    if (finalNewOwner.isOnline() && finalNewOwner.getPlayer() != null) {
                        StatHandler.addBlock(finalNewOwner.getPlayer(), target.getLocation());
                    }
                    String name = finalNewOwner.getName() != null ? finalNewOwner.getName() : targetName;
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TRANSFER_SUCCESS)
                            .replace("{player}", name)));
                } else {
                    player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(
                        Translator.get(TranslationKey.MESSAGES__TRANSFER_NOT_OWNER)));
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
