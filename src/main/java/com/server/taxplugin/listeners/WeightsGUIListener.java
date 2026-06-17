package com.server.taxplugin.listeners;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.gui.WeightsGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class WeightsGUIListener implements Listener {

    private final TaxPlugin plugin;

    public WeightsGUIListener(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!WeightsGUI.TITLE.equals(event.getView().getTitle())) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("taxplugin.admin")) return;

        int delta;
        ClickType click = event.getClick();
        if (click == ClickType.LEFT) {
            delta = 5;
        } else if (click == ClickType.RIGHT) {
            delta = -5;
        } else if (click == ClickType.SHIFT_LEFT) {
            delta = 1;
        } else if (click == ClickType.SHIFT_RIGHT) {
            delta = -1;
        } else {
            return;
        }

        WeightsGUI gui = new WeightsGUI(plugin);
        gui.adjustWeight(clicked.getType(), delta);
        gui.open(player);
    }
}
