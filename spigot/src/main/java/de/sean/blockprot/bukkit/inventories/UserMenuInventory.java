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
 * Items centred on row 1 (slots 9–17 of a triple-line inventory).
 *
 * Removed: Transfer and Timed (accessible via /bp transfer and /bp timed).
 * Hints toggle is now only in My Settings.
 */
public class UserMenuInventory extends BlockProtInventory {

    private static final int SLOT_SETTINGS = 11;
    private static final int SLOT_FRIENDS  = 12;
    private static final int SLOT_STATS    = 13;
    private static final int SLOT_ABOUT    = 14;

    public UserMenuInventory() { super(false); }

    @Override int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__USER_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();

        inventory.setItem(SLOT_SETTINGS, item(Material.WRITABLE_BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__SETTINGS_LORE)));
        inventory.setItem(SLOT_FRIENDS, item(Material.PLAYER_HEAD,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__FRIENDS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__STATS_LORE)));
        inventory.setItem(SLOT_ABOUT, item(Material.NETHER_STAR,
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT),
            Translator.get(TranslationKey.INVENTORIES__USER_MENU__ABOUT_LORE)));

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_SETTINGS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new UserSettingsInventory().fill(player));
            new PlayerSettingsHandler(player).setHasPlayerInteractedWithMenu(true);
        } else if (slot == SLOT_FRIENDS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), newState);
            var inv = new FriendManageInventory().fill(player);
            if (inv != null) player.openInventory(inv);
        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));
        } else if (slot == SLOT_ABOUT) {
            player.closeInventory();
            player.performCommand("blockprot about");
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
