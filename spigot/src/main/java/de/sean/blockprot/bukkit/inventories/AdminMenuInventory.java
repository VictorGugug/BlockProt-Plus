package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.bukkit.BlockProt;
import de.sean.blockprot.bukkit.BlockProtAPI;
import de.sean.blockprot.bukkit.TranslationKey;
import de.sean.blockprot.bukkit.Translator;
import de.sean.blockprot.bukkit.tasks.UpdateChecker;
import org.bukkit.Bukkit;
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
 * GUI for admin-only operations. Opened via /bp admin.
 * Requires blockprot.user.admin or OP.
 */
public class AdminMenuInventory extends BlockProtInventory {

    // Items centred in row 1 of a triple-line inventory (slots 10-16).
    private static final int SLOT_RELOAD       = 10;
    private static final int SLOT_UPDATE       = 11;
    private static final int SLOT_INTEGRATIONS = 12;
    private static final int SLOT_STATS        = 13;
    private static final int SLOT_DEBUG        = 14;
    private static final int SLOT_INFO         = 15;

    public AdminMenuInventory() {
        super(false);
    }

    @Override
    int getSize() { return InventoryConstants.tripleLine; }

    @Override
    String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__TITLE);
    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        inventory = createInventory();

        inventory.setItem(SLOT_RELOAD, item(Material.COMPARATOR,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__RELOAD_LORE)));
        inventory.setItem(SLOT_UPDATE, item(Material.SPYGLASS,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__UPDATE_LORE)));
        inventory.setItem(SLOT_INTEGRATIONS, item(Material.CHAIN,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INTEGRATIONS_LORE)));
        inventory.setItem(SLOT_STATS, item(Material.BOOK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__STATS_LORE)));
        inventory.setItem(SLOT_DEBUG, item(Material.COMMAND_BLOCK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__DEBUG_LORE)));
        inventory.setItem(SLOT_INFO, item(Material.GRASS_BLOCK,
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO),
            Translator.get(TranslationKey.INVENTORIES__ADMIN_MENU__INFO_LORE)));

        return inventory;
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= getSize()) return;

        if (slot == SLOT_RELOAD) {
            player.closeInventory();
            new de.sean.blockprot.bukkit.tasks.BackupTask(
                BlockProt.getInstance().getDataFolder(), true).run();
            BlockProt.getInstance().reloadConfigAndTranslations();
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__ADMIN_RELOAD_DONE));
        } else if (slot == SLOT_UPDATE) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskAsynchronously(BlockProt.getInstance(),
                new UpdateChecker(BlockProt.getInstance().getDescription(),
                    new ArrayList<>(Bukkit.getOnlinePlayers())));
        } else if (slot == SLOT_INTEGRATIONS) {
            player.closeInventory();
            var list = BlockProtAPI.getInstance().getIntegrations().stream()
                .filter(i -> i.isEnabled())
                .map(i -> "§6" + i.name).toList();
            String names = list.isEmpty() ? "§8(none)" : String.join("§7, ", list);
            String msg = Translator.get(TranslationKey.MESSAGES__ADMIN_INTEGRATIONS)
                .replace("{count}", String.valueOf(list.size()))
                .replace("{integrations}", names);
            player.sendMessage(msg);
        } else if (slot == SLOT_STATS) {
            InventoryState newState = new InventoryState(null);
            newState.friendSearchState = InventoryState.FriendSearchState.DEFAULT_FRIEND_SEARCH;
            InventoryState.set(player.getUniqueId(), newState);
            player.openInventory(new StatisticsInventory().fill(player));
        } else if (slot == SLOT_DEBUG) {
            player.closeInventory();
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__ADMIN_DEBUG_HINT));
            player.performCommand("blockprot debug run");
        } else if (slot == SLOT_INFO) {
            player.closeInventory();
            player.sendMessage(Translator.get(TranslationKey.MESSAGES__ADMIN_INFO_HINT));
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
