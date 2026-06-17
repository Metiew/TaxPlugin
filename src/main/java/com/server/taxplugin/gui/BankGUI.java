package com.server.taxplugin.gui;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.models.BankEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class BankGUI {

    public static final String TITLE_PREFIX = "§2Banca Virtuale - ";

    private final TaxPlugin plugin;

    public BankGUI(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        UUID owner = player.getUniqueId();
        List<BankEntry> entries = plugin.getDatabaseManager().getBankEntries(owner);

        int configuredSize = plugin.getConfig().getInt("bank-gui-size", 54);
        int size = (configuredSize > 0 && configuredSize % 9 == 0) ? configuredSize : 54;
        Inventory inv = Bukkit.createInventory(null, size, TITLE_PREFIX + player.getName());

        for (BankEntry entry : entries) {
            if (entry.getAmount() <= 0) continue;

            int displayAmount = (int) Math.min(entry.getAmount(), entry.getMaterial().getMaxStackSize());
            ItemStack display = new ItemStack(entry.getMaterial(), Math.max(1, displayAmount));
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setLore(List.of(
                        "§7Quantità accumulata: §e" + entry.getAmount(),
                        "",
                        "§aClicca per ritirare uno stack"
                ));
                display.setItemMeta(meta);
            }

            int firstEmpty = inv.firstEmpty();
            if (firstEmpty == -1) break;
            inv.setItem(firstEmpty, display);
        }

        player.openInventory(inv);
    }
}
