/*
 * Copyright (C) 2021 - 2025 spnda / SP26 fork
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

package de.sean.blockprot.bukkit;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Console helper that emits colored messages through the Bukkit console sender.
 *
 * <p>Paper's Log4j2 console renderer honors legacy Minecraft color codes (§x).
 * Messages sent to {@code Bukkit.getConsoleSender()} are displayed with colors
 * in Paper 1.21+ and fall back gracefully to plain text on vanilla Spigot terminals.
 *
 * <p>Color palette used by BlockProt-Plus:
 * <ul>
 *   <li>§6 Gold/amber  — startup phase banners and integration lines</li>
 *   <li>§e Yellow      — informational messages</li>
 *   <li>§c Red         — warnings / errors</li>
 *   <li>§7 Gray        — secondary / detail text</li>
 *   <li>§a Green       — success / up-to-date</li>
 *   <li>§r Reset       — return to default</li>
 * </ul>
 */
public final class BlockProtConsole {

    private static final String PREFIX = "§6[BlockProt]§r ";

    private BlockProtConsole() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** Prints a gold-prefixed informational line to the console. */
    public static void info(@NotNull String message) {
        send(PREFIX + "§e" + message + "§r");
    }

    /** Prints a gold-prefixed success line (green text). */
    public static void success(@NotNull String message) {
        send(PREFIX + "§a" + message + "§r");
    }

    /** Prints a gold-prefixed warning line (red text). */
    public static void warn(@NotNull String message) {
        send(PREFIX + "§c" + message + "§r");
    }

    /**
     * Prints the "integration registered" line in amber/brown tones.
     * Example output: §6[BlockProt]§r §6Integration: §eclaimchunk §7registered
     *
     * @param integrationName The plugin id (e.g. "claimchunk").
     */
    public static void integration(@NotNull String integrationName) {
        send(PREFIX + "§6Integration: §e" + integrationName + " §7registered§r");
    }

    /**
     * Prints an "integration enabled" confirmation.
     *
     * @param integrationName The plugin id.
     */
    public static void integrationEnabled(@NotNull String integrationName) {
        send(PREFIX + "§6Integration enabled: §e" + integrationName + "§r");
    }

    /**
     * Prints a generic banner line in amber (used for startup section headers).
     *
     * @param message The text to display.
     */
    public static void banner(@NotNull String message) {
        send("§6" + message + "§r");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private
    // ─────────────────────────────────────────────────────────────────────────

    private static void send(@NotNull String message) {
        Bukkit.getConsoleSender().sendMessage(message);
    }
}
