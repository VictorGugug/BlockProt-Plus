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
import de.sean.blockprot.bukkit.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Map;

/**
 * Main dispatcher for /blockprot (alias /bp).
 *
 * <p>Only two public subcommands exist:
 * <ul>
 *   <li>{@code user}  — opens the user GUI;  requires {@code blockprot.user} (default: true)</li>
 *   <li>{@code admin} — opens the admin GUI; requires {@code blockprot.user.admin} (default: op)</li>
 * </ul>
 *
 * <p>All legacy subcommands (help, settings, friends, reload, …) are still registered internally
 * so that the underlying command classes continue to work if invoked directly from other plugins
 * or scripts, but they are NOT shown in tab-complete.
 *
 * @since 1.1.2
 */
public final class BlockProtCommand implements TabExecutor {

    /** Publicly advertised commands (shown in tab-complete). */
    private static final Map<String, CommandExecutor> publicExecutors = new LinkedHashMap<>();

    /** All commands including legacy ones (not shown in tab-complete). */
    private static final Map<String, CommandExecutor> allExecutors = new LinkedHashMap<>();

    static {
        // ── Public GUI commands ──────────────────────────────────────────────
        register(true,  "user",         new UserMenuCommand());
        register(true,  "admin",        new AdminMenuCommand());

        // ── Legacy commands (hidden from tab-complete, still functional) ─────
        register(false, "stats",        new StatisticsCommand());
        register(false, "settings",     new SettingsCommand());
        register(false, "about",        new AboutCommand());
        register(false, "update",       new UpdateCommand());
        register(false, "reload",       new ReloadCommand());
        register(false, "integrations", new IntegrationsCommand());
        register(false, "debug",        new DebugCommand());
        register(false, "disablehints", new HintsCommand());
        register(false, "friends",      new FriendsAddAllCommand());
        register(false, "transfer",     new TransferCommand());
        register(false, "timed",        new TimedAccessCommand());
        register(false, "info",         new InfoCommand());
        register(false, "help",         new HelpCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Default: open the appropriate GUI only when use_menus=true
            if (BlockProt.getDefaultConfig().areExtraCommandsEnabled()) return false;
            if (sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key())) {
                return allExecutors.get("admin").onCommand(sender, command, label, args);
            }
            return allExecutors.get("user").onCommand(sender, command, label, args);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        boolean extraEnabled = BlockProt.getDefaultConfig().areExtraCommandsEnabled();
        if (!extraEnabled && !sub.equals("user") && !sub.equals("admin")) {
            return false;
        }

        var executor = allExecutors.get(sub);
        if (executor != null) {
            return executor.onCommand(sender, command, label, args);
        }
        return false;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                               @NotNull String alias, @NotNull String[] args) {
        if (args.length <= 1) {
            final var list = new ArrayList<String>();
            boolean extraEnabled = BlockProt.getDefaultConfig().areExtraCommandsEnabled();
            // When use_menus=false (extraCommands active): show all CLI commands, hide user/admin GUI.
            // When use_menus=true  (menus active):         show user/admin GUI only.
            for (var entry : allExecutors.entrySet()) {
                String sub = entry.getKey();
                if (extraEnabled && (sub.equals("user") || sub.equals("admin"))) continue;
                if (!extraEnabled && !publicExecutors.containsKey(sub)) continue;
                if (entry.getValue().canUseCommand(sender))
                    list.add(sub);
            }
            String partial = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            list.removeIf(s -> !s.startsWith(partial));
            return list;
        }

        var executor = allExecutors.get(args[0].toLowerCase(Locale.ROOT));
        if (executor != null) {
            final var completions = executor.onTabComplete(sender, command, alias, args);
            if (completions != null) return completions;
        }
        return Collections.emptyList();
    }

    private static void register(boolean isPublic, @NotNull String canonical,
                                 @NotNull CommandExecutor executor, String... aliases) {
        if (isPublic) publicExecutors.put(canonical, executor);
        allExecutors.put(canonical, executor);
        for (String alias : aliases) {
            allExecutors.put(alias.toLowerCase(Locale.ROOT), executor);
        }
    }
}
