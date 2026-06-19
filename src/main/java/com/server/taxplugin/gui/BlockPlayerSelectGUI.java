package com.server.taxplugin.gui;

import com.server.taxplugin.TaxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class BlockPlayerSelectGUI {

    public static final String TITLE = "§6Seleziona giocatore - /block";

    private final TaxPlugin plugin;

    public BlockPlayerSelectGUI(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        List<Player> online = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
        int size = Math.min(54, Math.max(9, ((online.size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(null, size, TITLE);

        for (Player target : online) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName("§e" + target.getName());
                meta.setLore(List.of(
                        "§7Clicca per gestire l'armatura",
                        "§7esente dalla tassazione di questo giocatore"
                ));
                head.setItemMeta(meta);
            }

            int firstEmpty = inv.firstEmpty();
            if (firstEmpty == -1) break;
            inv.setItem(firstEmpty, head);
        }

        viewer.openInventory(inv);
    }
}
