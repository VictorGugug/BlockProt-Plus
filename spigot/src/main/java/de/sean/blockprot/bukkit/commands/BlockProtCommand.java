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

import de.sean.blockprot.bukkit.Permissions;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
        register(false, "stats",        new StatisticsCommand(), "statistics");
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
            // Default: open the appropriate GUI
            if (sender.isOp() || sender.hasPermission(Permissions.USER_ADMIN.key())) {
                return allExecutors.get("admin").onCommand(sender, command, label, args);
            }
            return allExecutors.get("user").onCommand(sender, command, label, args);
        }

        var executor = allExecutors.get(args[0].toLowerCase(Locale.ROOT));
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
            for (var entry : publicExecutors.entrySet()) {
                if (entry.getValue().canUseCommand(sender))
                    list.add(entry.getKey());
            }
            // Filter by what they have typed so far
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
        // Spanish localized aliases for public commands
        addSpanishAliases(canonical, executor);
    }

    private static void addSpanishAliases(@NotNull String canonical, @NotNull CommandExecutor executor) {
        switch (canonical) {
            case "user"  -> allExecutors.put("usuario",       executor);
            case "admin" -> allExecutors.put("administrador", executor);
            // Legacy
            case "stats"        -> allExecutors.put("estadisticas",          executor);
            case "settings"     -> { allExecutors.put("ajustes", executor); allExecutors.put("configuracion", executor); }
            case "about"        -> allExecutors.put("acerca",                executor);
            case "update"       -> allExecutors.put("actualizar",            executor);
            case "reload"       -> allExecutors.put("recargar",              executor);
            case "integrations" -> allExecutors.put("integraciones",         executor);
            case "debug"        -> allExecutors.put("depurar",               executor);
            case "disablehints" -> allExecutors.put("desactivarsugerencias", executor);
            case "friends"      -> allExecutors.put("amigos",                executor);
            case "transfer"     -> allExecutors.put("transferir",            executor);
            case "timed"        -> allExecutors.put("temporal",              executor);
            case "help"         -> allExecutors.put("ayuda",                 executor);
        }
    }
}
