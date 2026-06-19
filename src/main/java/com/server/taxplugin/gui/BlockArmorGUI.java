package com.server.taxplugin.gui;

import com.server.taxplugin.TaxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class BlockArmorGUI {

    public static final String TITLE_PREFIX = "§6Armatura di ";

    private static final int SLOT_HELMET = 2;
    private static final int SLOT_CHESTPLATE = 3;
    private static final int SLOT_LEGGINGS = 4;
    private static final int SLOT_BOOTS = 5;

    private final TaxPlugin plugin;

    public BlockArmorGUI(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, Player target) {
        Inventory inv = Bukkit.createInventory(null, 9, TITLE_PREFIX + target.getName());

        var armor = target.getInventory();
        placeArmorSlot(inv, SLOT_HELMET, armor.getHelmet());
        placeArmorSlot(inv, SLOT_CHESTPLATE, armor.getChestplate());
        placeArmorSlot(inv, SLOT_LEGGINGS, armor.getLeggings());
        placeArmorSlot(inv, SLOT_BOOTS, armor.getBoots());

        viewer.openInventory(inv);
    }

    private void placeArmorSlot(Inventory inv, int slot, ItemStack piece) {
        if (piece == null || piece.getType() == Material.AIR) {
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = placeholder.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§7Nessun pezzo equipaggiato");
                placeholder.setItemMeta(meta);
            }
            inv.setItem(slot, placeholder);
            return;
        }

        ItemStack display = piece.clone();
        boolean exempt = plugin.getTaxManager().getTagger().isExempt(display);

        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new java.util.ArrayList<>();
            lore.add("");
            lore.add(exempt ? "§a✔ Esente dalla tassazione" : "§7Tassabile normalmente");
            lore.add("");
            lore.add(exempt ? "§cClicca per rimuovere l'esenzione" : "§aClicca per esentare questo pezzo");
            meta.setLore(lore);
            display.setItemMeta(meta);
        }

        inv.setItem(slot, display);
    }

    public void toggleExemptBySlot(Player target, int guiSlot) {
        var armor = target.getInventory();
        ItemStack piece = switch (guiSlot) {
            case SLOT_HELMET -> armor.getHelmet();
            case SLOT_CHESTPLATE -> armor.getChestplate();
            case SLOT_LEGGINGS -> armor.getLeggings();
            case SLOT_BOOTS -> armor.getBoots();
            default -> null;
        };

        if (piece == null || piece.getType() == Material.AIR) return;

        var tagger = plugin.getTaxManager().getTagger();
        if (tagger.isExempt(piece)) {
            tagger.unmarkExempt(piece);
        } else {
            tagger.markExempt(piece);
        }

        switch (guiSlot) {
            case SLOT_HELMET -> armor.setHelmet(piece);
            case SLOT_CHESTPLATE -> armor.setChestplate(piece);
            case SLOT_LEGGINGS -> armor.setLeggings(piece);
            case SLOT_BOOTS -> armor.setBoots(piece);
        }
    }

    public static boolean isArmorSlot(int slot) {
        return slot == SLOT_HELMET || slot == SLOT_CHESTPLATE || slot == SLOT_LEGGINGS || slot == SLOT_BOOTS;
    }
}
