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

package de.sean.blockprot.bukkit.nbt;

import de.sean.blockprot.bukkit.BlockProt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages time-limited friend access for protected blocks.
 *
 * <p>When a player grants timed access, a Bukkit task is scheduled to automatically
 * remove that friend once the duration elapses.  All state is in-memory: if the
 * server restarts the access grant is revoked immediately (safe-by-default).
 *
 * <p>Example usage:
 * <pre>{@code
 *   TimedAccessManager.grant(block.getLocation(), guestUuid, ownerUuid, 300); // 5 minutes
 *   TimedAccessManager.revoke(block.getLocation(), guestUuid);
 * }</pre>
 *
 * @since 1.2.0
 */
public final class TimedAccessManager {

    /** Key = "world:x,y,z:guestUuid" -&gt; running expiry task. */
    private static final Map<String, BukkitTask> tasks = new HashMap<>();

    private TimedAccessManager() {}

    /**
     * Grants {@code guest} access to the block at {@code location} for {@code durationSeconds}.
     * If a previous timed grant already exists for the same block+guest pair, the old timer is
     * cancelled and replaced with the new one.
     *
     * <p>Must be called on the main server thread.
     *
     * @param location        The location of the protected block.
     * @param guestUuid       The UUID of the player being granted temporary access.
     * @param ownerUuid       The UUID of the current block owner (used to verify ownership).
     * @param durationSeconds How long (in seconds) the access should last.
     * @return {@code true} if the friend was successfully added; {@code false} otherwise
     *         (block not found, caller is not owner, or block is not lockable).
     */
    public static boolean grant(@NotNull Location location,
                                @NotNull UUID guestUuid,
                                @NotNull UUID ownerUuid,
                                long durationSeconds) {
        if (location.getWorld() == null) return false;

        try {
            var block = location.getWorld().getBlockAt(location);
            var handler = new BlockNBTHandler(block);
            if (!handler.isOwner(ownerUuid)) return false;
            if (!handler.containsFriend(guestUuid.toString())) {
                handler.addFriend(guestUuid.toString());
                handler.applyToOtherContainer();
            }
        } catch (RuntimeException e) {
            return false;
        }

        // Cancel any existing timer for this pair before scheduling a new one.
        String key = buildKey(location, guestUuid);
        @Nullable BukkitTask old = tasks.remove(key);
        if (old != null) old.cancel();

        long ticks = durationSeconds * 20L;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(BlockProt.getInstance(), () -> {
            tasks.remove(key);
            revokeSilently(location, guestUuid);
        }, ticks);

        tasks.put(key, task);
        return true;
    }

    /**
     * Immediately revokes timed access for {@code guest} on the block at {@code location}
     * and cancels any pending expiry task.
     *
     * @param location  The location of the block.
     * @param guestUuid The guest whose access should be revoked.
     */
    public static void revoke(@NotNull Location location, @NotNull UUID guestUuid) {
        String key = buildKey(location, guestUuid);
        @Nullable BukkitTask task = tasks.remove(key);
        if (task != null) task.cancel();
        revokeSilently(location, guestUuid);
    }

    /**
     * Returns whether {@code guest} currently has a pending timed-access grant on the
     * block at {@code location}.
     */
    public static boolean hasPendingGrant(@NotNull Location location, @NotNull UUID guestUuid) {
        return tasks.containsKey(buildKey(location, guestUuid));
    }

    /**
     * Cancels ALL pending timed-access tasks.
     * Should be called during plugin disable so no stale tasks linger.
     */
    public static void cancelAll() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static void revokeSilently(@NotNull Location location, @NotNull UUID guestUuid) {
        if (location.getWorld() == null) return;
        try {
            var block = location.getWorld().getBlockAt(location);
            var handler = new BlockNBTHandler(block);
            if (handler.containsFriend(guestUuid.toString())) {
                handler.removeFriend(guestUuid.toString());
                handler.applyToOtherContainer();
            }
        } catch (RuntimeException ignored) {
            // Block may have been broken or is no longer lockable — nothing to do.
        }
    }

    @NotNull
    private static String buildKey(@NotNull Location loc, @NotNull UUID guest) {
        return loc.getWorld().getName()
            + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
            + ":" + guest;
    }
}
