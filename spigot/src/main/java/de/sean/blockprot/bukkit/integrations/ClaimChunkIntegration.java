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

package de.sean.blockprot.bukkit.integrations;

import com.cjburkey.claimchunk.ClaimChunk;
import com.cjburkey.claimchunk.chunk.ChunkHandler;
import com.cjburkey.claimchunk.chunk.ChunkPos;
import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.events.BlockAccessEvent;
import de.sean.blockprot.bukkit.events.BlockLockOnPlaceEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Integration with ClaimChunk (https://github.com/cjburkey01/ClaimChunk).
 *
 * <p>Prevents players from locking blocks inside chunks they do not own,
 * and optionally restricts access to blocks to chunk owners only.</p>
 *
 * @since SP26
 */
public final class ClaimChunkIntegration extends PluginIntegration implements Listener {

    private static final String RESTRICT_ACCESS_TO_CHUNK_OWNER = "restrict_access_to_chunk_owner";

    @Nullable
    private ClaimChunk claimChunk;

    private boolean enabled = false;

    public ClaimChunkIntegration() {
        super("claimchunk");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        Plugin plugin = getPlugin();
        if (plugin == null || !plugin.isEnabled()) return;

        claimChunk = (ClaimChunk) plugin;
        this.registerListener(this);
        enabled = true;
    }

    @Override
    public @Nullable Plugin getPlugin() {
        return BlockProt.getInstance().getPlugin("ClaimChunk");
    }

    /**
     * Whether only the chunk owner may access unprotected blocks inside claimed chunks.
     */
    private boolean shouldRestrictAccessToChunkOwner() {
        return configuration.contains(RESTRICT_ACCESS_TO_CHUNK_OWNER)
            && configuration.getBoolean(RESTRICT_ACCESS_TO_CHUNK_OWNER);
    }

    /**
     * Returns the UUID of whoever owns the chunk containing {@code block},
     * or {@code null} if the chunk is unclaimed.
     */
    @Nullable
    private UUID getChunkOwner(@NotNull Block block) {
        if (claimChunk == null) return null;
        ChunkHandler chunkHandler = claimChunk.getChunkHandler();
        if (chunkHandler == null) return null;
        org.bukkit.Chunk chunk = block.getChunk();
        ChunkPos pos = new ChunkPos(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        return chunkHandler.getOwner(pos);
    }

    // ----- filter friends -----

    @Override
    protected boolean filterFriendByUuid(@NotNull UUID friend,
                                         @NotNull Player player,
                                         @NotNull Block block) {
        if (claimChunk == null) return true;
        UUID owner = getChunkOwner(block);
        if (owner == null) return true; // Unclaimed chunk — no restriction.
        return friend.equals(owner);
    }

    // ----- events -----

    /**
     * Deny access to blocks inside a chunk the player does not own,
     * if restrict_access_to_chunk_owner is enabled.
     */
    @EventHandler
    public void onAccess(@NotNull BlockAccessEvent event) {
        if (claimChunk == null || !shouldRestrictAccessToChunkOwner()) return;
        UUID owner = getChunkOwner(event.getBlock());
        if (owner == null) return; // Unclaimed chunk — allow.
        if (!owner.equals(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevent lock-on-place inside a chunk the player does not own.
     */
    @EventHandler
    public void onLockOnPlace(@NotNull BlockLockOnPlaceEvent event) {
        if (claimChunk == null) return;
        UUID owner = getChunkOwner(event.getBlock());
        if (owner == null) return; // Unclaimed — allow.
        if (!owner.equals(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
