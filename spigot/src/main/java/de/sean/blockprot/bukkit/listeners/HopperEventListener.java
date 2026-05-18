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

package de.sean.blockprot.bukkit.listeners;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class HopperEventListener implements Listener {

    /**
     * Short-lived protection cache to avoid re-reading NBT on every hopper tick.
     *
     * <p>InventoryMoveItemEvent fires up to 20 times per second per hopper. Reading
     * NBT from disk on each call adds measurable overhead on busy farms. This cache
     * stores whether a block is protected, who owns it, and the hopper-protection flag,
     * keyed by world+coords. Entries expire after {@link #CACHE_TTL_MS} milliseconds
     * so changes (lock/unlock) are visible within one second without requiring an
     * explicit invalidation.</p>
     */
    private static final long CACHE_TTL_MS = 1_000L;

    private record CacheEntry(boolean isProtected, String owner, boolean hopperProtection, long expiresAt) {}

    // Concurrent because the scheduler may access this from different threads.
    private static final ConcurrentHashMap<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    /** Encodes world+coords into a single long key. Works for coords within ±1M blocks. */
    private static long cacheKey(@NotNull Block block) {
        // XZ are bounded to 30M blocks max; Y to 2048. Bit-packing is safe.
        long key = block.getWorld().getUID().getLeastSignificantBits();
        key ^= (long) (block.getX() + 30_000_000) << 32;
        key ^= (long) (block.getZ() + 30_000_000) << 11;
        key ^= (long) (block.getY() + 2048);
        return key;
    }

    /**
     * Returns a cache entry for the given block, reading NBT only on cache miss or expiry.
     * Must be called from the server main thread (NBT access is not thread-safe).
     */
    @Nullable
    private CacheEntry getEntry(@Nullable Block block) {
        if (block == null) return null;
        long key = cacheKey(block);
        CacheEntry entry = cache.get(key);
        if (entry != null && System.currentTimeMillis() < entry.expiresAt()) return entry;

        // Cache miss or stale — read NBT and populate.
        try {
            BlockNBTHandler handler = new BlockNBTHandler(block);
            entry = new CacheEntry(
                handler.isProtected(),
                handler.getOwner(),
                handler.getRedstoneHandler().getHopperProtection(),
                System.currentTimeMillis() + CACHE_TTL_MS
            );
            cache.put(key, entry);
            return entry;
        } catch (RuntimeException e) {
            // Block is not lockable; do not cache to avoid polluting the map.
            return null;
        }
    }

    /**
     * Invalidates the cache entry for a block. Call this whenever a block's
     * protection state changes (lock, unlock, clear).
     */
    public static void invalidate(@NotNull Block block) {
        cache.remove(cacheKey(block));
    }

    @EventHandler
    public void onItemMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() == null) return;
        if (BlockProt.getDefaultConfig().isWorldExcluded(event.getSource().getHolder())) return;
        if (event.getDestination().getType() != InventoryType.HOPPER
                && event.getSource().getType() != InventoryType.HOPPER) return;

        Block source = getBlock(event.getSource().getHolder());
        if (source == null) return;
        if (!BlockProt.getDefaultConfig().isLockable(source.getType())) return;

        CacheEntry sourceEntry = getEntry(source);
        if (sourceEntry == null || !sourceEntry.isProtected()) return;

        // Fast-path: hopper protection is disabled on the source — allow the move.
        if (!sourceEntry.hopperProtection()) return;

        // Source is protected and hopper-protection is on.
        // Allow the move only when the destination is a container owned by the same player.
        InventoryHolder destinationHolder = event.getDestination().getHolder();
        if (destinationHolder instanceof Container || destinationHolder instanceof DoubleChest) {
            Block destination = getBlock(destinationHolder);
            if (destination != null && BlockProt.getDefaultConfig().isLockable(destination.getType())) {
                CacheEntry destEntry = getEntry(destination);
                // Same owner — allow regardless of destination protection state.
                if (destEntry != null && destEntry.owner().equals(sourceEntry.owner())) return;
            }
        }

        // All other cases (different owner, unprotected destination, minecart, etc.) — block.
        event.setCancelled(true);
    }

    @Nullable
    private Block getBlock(InventoryHolder holder) {
        if (holder instanceof Container) return ((Container) holder).getBlock();
        else if (holder instanceof DoubleChest doubleChest) {
            if (doubleChest.getWorld() == null) return null;
            return doubleChest.getWorld().getBlockAt(doubleChest.getLocation());
        } else return null;
    }
}
