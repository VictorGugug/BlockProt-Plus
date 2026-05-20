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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Console helper that routes BlockProt messages through the Bukkit console sender.
 *
 * <p>During startup, messages are buffered via {@link #startupBuffer} and printed
 * together inside the ASCII chest banner at the end of {@code onEnable}.
 * After startup, all methods print immediately as plain text.
 */
public final class BlockProtConsole {

    /**
     * When non-null, startup messages are collected here instead of being
     * printed immediately. Flushed and cleared by {@link #printStartupBanner}.
     */
    @Nullable
    private static List<String> startupBuffer = null;

    /**
     * Plugin logger, set by {@link #beginStartup(java.util.logging.Logger)}.
     * Used to print each banner line so every line gets the standard
     * {@code [HH:MM:SS INFO]: [BlockProt] } prefix, exactly like SkinsRestorer.
     */
    @Nullable
    private static Logger pluginLogger = null;

    private BlockProtConsole() {}

    // -------------------------------------------------------------------------
    // Startup buffer control
    // -------------------------------------------------------------------------

    /** Activates startup buffering. Call at the very start of {@code onEnable}. */
    public static void beginStartup(@NotNull Logger logger) {
        pluginLogger = logger;
        startupBuffer = new ArrayList<>();
    }

    /**
     * Prints a simple startup summary, then clears the buffer so subsequent calls print immediately.
     *
     * @param version The plugin version string.
     */
    public static void printStartupBanner(@NotNull String version) {
        List<String> lines = startupBuffer != null ? startupBuffer : new ArrayList<>();
        startupBuffer = null; // exit buffering mode

        log("BlockProt v" + version + " enabled.");
        for (String line : lines) {
            log(line);
        }
    }

    /** Sends one banner line via the plugin logger. */
    private static void log(@NotNull String line) {
        if (pluginLogger != null) {
            pluginLogger.info(line);
        } else {
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Informational line — buffered during startup, immediate otherwise. */
    public static void info(@NotNull String message) {
        emit(message);
    }

    /** Success line — buffered during startup, immediate otherwise. */
    public static void success(@NotNull String message) {
        emit(message);
    }

    /** Warning line — always printed immediately (never buffered). */
    public static void warn(@NotNull String message) {
        Bukkit.getConsoleSender().sendMessage("[BlockProt] WARN: " + message);
    }

    /**
     * "Integration registered" line — buffered during startup, immediate otherwise.
     *
     * @param integrationName The plugin id (e.g. "claimchunk").
     */
    public static void integration(@NotNull String integrationName) {
        emit("Integration: " + integrationName + " registered");
    }

    /**
     * "Integration enabled" confirmation — buffered during startup, immediate otherwise.
     *
     * @param integrationName The plugin id.
     */
    public static void integrationEnabled(@NotNull String integrationName) {
        emit("Integration enabled: " + integrationName);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Buffers the message during startup; prints it immediately (with prefix) after.
     */
    private static void emit(@NotNull String message) {
        if (startupBuffer != null) {
            startupBuffer.add(message);
        } else {
            Bukkit.getConsoleSender().sendMessage("[BlockProt] " + message);
        }
    }

}
