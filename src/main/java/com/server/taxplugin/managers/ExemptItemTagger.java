package com.server.taxplugin.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ExemptItemTagger {

    private final NamespacedKey exemptKey;

    public ExemptItemTagger(JavaPlugin plugin) {
        this.exemptKey = new NamespacedKey(plugin, "tax_exempt");
    }

    public void markExempt(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(exemptKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public void unmarkExempt(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(exemptKey);
        item.setItemMeta(meta);
    }

    public boolean isExempt(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(exemptKey, PersistentDataType.BYTE);
    }
}
