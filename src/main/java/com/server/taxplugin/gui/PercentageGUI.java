package com.server.taxplugin.gui;

import com.server.taxplugin.TaxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PercentageGUI {

    public static final String TITLE = "§6Percentuale Tassa Globale";

    private static final int SLOT_MINUS_5 = 1;
    private static final int SLOT_MINUS_1 = 3;
    private static final int SLOT_DISPLAY = 4;
    private static final int SLOT_PLUS_1 = 5;
    private static final int SLOT_PLUS_5 = 7;

    private final TaxPlugin plugin;

    public PercentageGUI(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE);

        inv.setItem(SLOT_MINUS_5, namedItem(Material.RED_CONCRETE, "§c-5%"));
        inv.setItem(SLOT_MINUS_1, namedItem(Material.RED_STAINED_GLASS_PANE, "§c-1%"));
        inv.setItem(SLOT_DISPLAY, displayItem());
        inv.setItem(SLOT_PLUS_1, namedItem(Material.LIME_STAINED_GLASS_PANE, "§a+1%"));
        inv.setItem(SLOT_PLUS_5, namedItem(Material.LIME_CONCRETE, "§a+5%"));

        player.openInventory(inv);
    }

    public void adjustPercentage(double delta) {
        double current = plugin.getConfig().getDouble("tax-percentage", 10.0);
        double updated = Math.max(0, Math.min(100, current + delta));
        plugin.getConfig().set("tax-percentage", updated);
        plugin.saveConfig();
    }

    private ItemStack displayItem() {
        double current = plugin.getConfig().getDouble("tax-percentage", 10.0);
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eTassa attuale: §6" + current + "%");
            meta.setLore(List.of("§7Usa i pulsanti per modificare la percentuale."));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack namedItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
