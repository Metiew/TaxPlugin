package com.server.taxplugin.listeners;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.gui.PercentageGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PercentageGUIListener implements Listener {

    private final TaxPlugin plugin;

    public PercentageGUIListener(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(PercentageGUI.TITLE_PREFIX)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!viewer.hasPermission("taxplugin.admin")) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        double delta;
        switch (meta.getDisplayName()) {
            case "§c-5%" -> delta = -5;
            case "§c-1%" -> delta = -1;
            case "§a+1%" -> delta = 1;
            case "§a+5%" -> delta = 5;
            default -> { return; }
        }

        String targetName = title.substring(PercentageGUI.TITLE_PREFIX.length());
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getUniqueId() == null) return;

        PercentageGUI gui = new PercentageGUI(plugin, target.getUniqueId(), targetName);
        gui.adjustPercentage(delta);
        gui.open(viewer);
    }
}
