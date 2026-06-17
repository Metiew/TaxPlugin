package com.server.taxplugin.gui;

import com.server.taxplugin.TaxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class WeightsGUI {

    public static final String TITLE = "§6Pesi Tassazione - Item";

    private final TaxPlugin plugin;

    public WeightsGUI(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Map<String, Integer> weights = loadWeightsSorted();
        int size = Math.min(54, Math.max(9, ((weights.size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(null, size, TITLE);

        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            Material material = Material.matchMaterial(entry.getKey());
            if (material == null) continue;

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e" + material.name());
                meta.setLore(List.of(
                        "§7Peso attuale: §a" + entry.getValue(),
                        "",
                        "§aClick sinistro: §7+5 peso",
                        "§cClick destro: §7-5 peso",
                        "§aShift+sinistro: §7+1 peso",
                        "§cShift+destro: §7-1 peso"
                ));
                item.setItemMeta(meta);
            }

            int firstEmpty = inv.firstEmpty();
            if (firstEmpty == -1) break;
            inv.setItem(firstEmpty, item);
        }

        player.openInventory(inv);
    }

    private Map<String, Integer> loadWeightsSorted() {
        Map<String, Integer> result = new TreeMap<>();
        var section = plugin.getConfig().getConfigurationSection("item-weights");
        if (section == null) return result;
        for (String key : section.getKeys(false)) {
            result.put(key, section.getInt(key));
        }
        return result;
    }

    public void adjustWeight(Material material, int delta) {
        String path = "item-weights." + material.name();
        int current = plugin.getConfig().getInt(path, 0);
        int updated = Math.max(0, Math.min(100, current + delta));
        plugin.getConfig().set(path, updated);
        plugin.saveConfig();
    }
}
