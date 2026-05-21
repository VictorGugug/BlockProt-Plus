package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
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

/**
 * GUI for regular user operations. Opened via /bp user.
 * Requires blockprot.user (default: true for all players).
 */
public class UserMenuInventory extends BlockProtInventory {

    public UserMenuInventory() {
        super(false);
    }

    @Override
    int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();

        inventory.setItem(0, item(Material.WRITABLE_BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS_LORE)));
        inventory.setItem(1, item(Material.PLAYER_HEAD,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS_LORE)));
        inventory.setItem(2, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS_LORE)));
        inventory.setItem(3, item(Material.ENDER_PEARL,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TRANSFER_LORE)));
        inventory.setItem(4, item(Material.CLOCK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TIMED),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__TIMED_LORE)));
        inventory.setItem(5, item(Material.KNOWLEDGE_BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__HINTS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__HINTS_LORE)));
        inventory.setItem(6, item(Material.NETHER_STAR,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT_LORE)));
        inventory.setItem(8, item(Material.BARRIER,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__CLOSE),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__CLOSE_LORE)));

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        switch (slot) {
            case 0 -> {
                InventoryState newState = new InventoryState(null);
                newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), newState);
                player.openInventory(new UserSettingsInventory().fill(player));
                new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
            }
            case 1 -> {
                InventoryState newState = new InventoryState(null);
                newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), newState);
                var inv = new FriendManageInventory().fill(player);
                if (inv != null) player.openInventory(inv);
            }
            case 2 -> {
                InventoryState newState = new InventoryState(null);
                newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
                InventoryState.set(player.getUniqueId(), newState);
                player.openInventory(new StatisticsInventory().fill(player));
            }
            case 3 -> {
                player.closeInventory();
                player.sendMessage(Translator.get(TranslationKey.MESSAGES__USER_TRANSFER_HINT));
            }
            case 4 -> {
                player.closeInventory();
                player.sendMessage(Translator.get(TranslationKey.MESSAGES__USER_TIMED_HINT));
            }
            case 5 -> {
                player.closeInventory();
                var settings = new PlayerSettingsHandler(player);
                if (!settings.hasPlayerInteractedWithMenu()) {
                    settings.setHasPlayerInteractedWithMenu(true);
                    player.sendMessage(Translator.get(TranslationKey.MESSAGES__HINTS_DISABLED));
                } else {
                    player.sendMessage(Translator.get(TranslationKey.MESSAGES__HINTS_ALREADY));
                }
            }
            case 6 -> {
                player.closeInventory();
                player.performCommand("blockprot about");
            }
            case 8 -> player.closeInventory();
        }
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {}

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(s);
        meta.setLore(l);
        item.setItemMeta(meta);
        return item;
    }
}
