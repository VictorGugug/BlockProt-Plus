package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.TimedAccessManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Inventory for selecting a timed-access duration for a specific friend on a specific block.
 *
 * <p>Layout (3 rows = 27 slots):
 * <pre>
 *  Row 0 (presets):  30s | 5min | 30min | 1h | 6h | 12h | 1d | 3d | 7d
 *  Row 1 (presets): 14d | 30d | 60d | 90d | --- | --- | --- | --- | ---
 *  Row 2 (nav):   [back] | --- | --- | --- | --- | --- | --- | --- | [info]
 * </pre>
 */
public final class TimedAccessInventory extends BlockProtInventory {

    /** Preset durations in seconds. */
    private static final long[] PRESETS = {
        30,
        5 * 60,
        30 * 60,
        60 * 60,
        6 * 60 * 60,
        12 * 60 * 60,
        86400,
        3 * 86400,
        7 * 86400,
        14 * 86400,
        30 * 86400,
        60 * 86400,
        90 * 86400,
    };

    @Override
    int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__TIMED__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();
        return fillForPlayer(player);
    }

    @NotNull
    public Inventory fillForPlayer(@NotNull Player player) {
        inventory.clear();

        long maxSeconds = BlockProt.getDefaultConfig().getTimedAccessMaxDurationSeconds();

        InventoryState state = InventoryState.get(player.getUniqueId());
        String friendName = "?";
        if (state != null && state.currentFriend != null) {
            friendName = tryGetName(state.currentFriend);
        }

        for (int i = 0; i < PRESETS.length; i++) {
            long secs = PRESETS[i];
            boolean overMax = maxSeconds != Long.MAX_VALUE && secs > maxSeconds;
            Material mat = overMax ? Material.RED_STAINED_GLASS_PANE : Material.CLOCK;
            String label = (overMax ? "§c" : "§a") + formatDuration(secs);
            List<String> lore = new ArrayList<>();
            if (overMax) lore.add(Translator.get(TranslationKey.INVENTORIES__TIMED__OVER_MAX));
            inventory.setItem(i, item(mat, label, lore));
        }

        // Custom duration button: slot 13 (centre of row 1)
        inventory.setItem(13, item(Material.WRITABLE_BOOK,
            "§e" + Translator.get(TranslationKey.INVENTORIES__TIMED__BUTTON),
            List.of("§7" + Translator.get(TranslationKey.INVENTORIES__TIMED__BUTTON_LORE))));

        // Info slot: slot 26
        List<String> infoLore = new ArrayList<>();
        infoLore.add(Translator.get(TranslationKey.INVENTORIES__TIMED__PLAYER_LABEL).replace("{player}", friendName));
        if (maxSeconds != Long.MAX_VALUE) {
            infoLore.add(Translator.get(TranslationKey.INVENTORIES__TIMED__MAX_LABEL).replace("{duration}", formatDuration(maxSeconds)));
        } else {
            infoLore.add(Translator.get(TranslationKey.INVENTORIES__TIMED__NO_LIMIT));
        }
        inventory.setItem(26, item(Material.NETHER_STAR, Translator.get(TranslationKey.INVENTORIES__TIMED__INFO), infoLore));

        setBackButton();
        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        // Back button: slot 18
        if (slot == getSize() - 9) {
            closeAndOpen(player, new FriendDetailInventory().fill(player));
            return;
        }

        // Custom duration input: slot 13
        if (slot == 13) {
            player.closeInventory();
            AnvilInput.open(player, BlockProt.getInstance(),
                "", Translator.get(TranslationKey.INVENTORIES__TIMED__BUTTON),
                text -> {
                    if (text == null || text.isBlank()) return;
                    long secs = parseDurationInput(text);
                    if (secs <= 0) {
                        player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_INVALID_STATE));
                        return;
                    }
                    long maxSecs = BlockProt.getDefaultConfig().getTimedAccessMaxDurationSeconds();
                    if (maxSecs != Long.MAX_VALUE && secs > maxSecs) {
                        player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_OVER_MAX)
                            .replace("{duration}", formatDuration(maxSecs)));
                        return;
                    }
                    InventoryState invState = InventoryState.get(player.getUniqueId());
                    if (invState == null || invState.currentFriend == null || invState.getBlock() == null) {
                        player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_INVALID_STATE));
                        return;
                    }
                    boolean granted = TimedAccessManager.grant(
                        invState.getBlock().getLocation(),
                        invState.currentFriend,
                        player.getUniqueId(),
                        secs
                    );
                    if (granted) {
                        player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_GRANTED)
                            .replace("{duration}", formatDuration(secs)));
                    } else {
                        player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_NOT_OWNER));
                    }
                    var inv = new FriendDetailInventory().fill(player);
                    if (inv != null) player.openInventory(inv);
                });
            return;
        }

        if (slot >= PRESETS.length) return;

        long secs = PRESETS[slot];
        long maxSecs = BlockProt.getDefaultConfig().getTimedAccessMaxDurationSeconds();
        if (maxSecs != Long.MAX_VALUE && secs > maxSecs) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_OVER_MAX)
                .replace("{duration}", formatDuration(maxSecs)));
            return;
        }

        if (state.currentFriend == null || state.getBlock() == null) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_INVALID_STATE));
            player.closeInventory();
            return;
        }

        boolean granted = TimedAccessManager.grant(
            state.getBlock().getLocation(),
            state.currentFriend,
            player.getUniqueId(),
            secs
        );

        if (granted) {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_GRANTED)
                .replace("{duration}", formatDuration(secs)));
        } else {
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__TIMED_ACCESS_NOT_OWNER));
        }
        closeAndOpen(player, new FriendDetailInventory().fill(player));
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    // ── helpers ───────────────────────────────────────────────────────────────

    public static String formatDuration(long totalSeconds) {
        if (totalSeconds >= 86400) {
            long days = totalSeconds / 86400;
            long rem  = totalSeconds % 86400;
            if (rem == 0) return days + (days == 1 ? " day" : " days");
            return days + "d " + formatDuration(rem);
        }
        if (totalSeconds >= 3600) {
            long hours = totalSeconds / 3600;
            long rem   = totalSeconds % 3600;
            if (rem == 0) return hours + (hours == 1 ? " hour" : " hours");
            return hours + "h " + formatDuration(rem);
        }
        if (totalSeconds >= 60) {
            long mins = totalSeconds / 60;
            long rem  = totalSeconds % 60;
            if (rem == 0) return mins + " min";
            return mins + "min " + rem + "s";
        }
        return totalSeconds + "s";
    }

    /**
     * Parses a human-readable duration string into seconds.
     * Accepted formats: "30s", "5m", "5min", "2h", "1d", "7d", or a plain number (treated as seconds).
     * Returns 0 or negative if the input is invalid.
     */
    public static long parseDurationInput(@NotNull String input) {
        input = input.trim().toLowerCase();
        try {
            // Plain number → seconds
            return Long.parseLong(input);
        } catch (NumberFormatException ignored) {}
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile("(\\d+)\\s*([smhd](?:in)?)").matcher(input);
        long total = 0;
        boolean matched = false;
        while (m.find()) {
            long val = Long.parseLong(m.group(1));
            String unit = m.group(2);
            total += switch (unit) {
                case "s"         -> val;
                case "m", "min"  -> val * 60;
                case "h"         -> val * 3600;
                case "d"         -> val * 86400;
                default          -> 0;
            };
            matched = true;
        }
        return matched ? total : -1;
    }

    private String tryGetName(UUID uuid) {
        try {
            var profile = BlockProt.getProfileService().findByUuid(uuid);
            return profile != null && profile.getName() != null
                ? profile.getName()
                : uuid.toString().substring(0, 8);
        } catch (Exception e) {
            return uuid.toString().substring(0, 8);
        }
    }

    private ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
