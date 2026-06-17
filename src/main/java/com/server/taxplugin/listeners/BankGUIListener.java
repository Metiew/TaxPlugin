package com.server.taxplugin.listeners;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.gui.BankGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BankGUIListener implements Listener {

    private final TaxPlugin plugin;

    public BankGUIListener(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.startsWith(BankGUI.TITLE_PREFIX)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int maxStack = clicked.getMaxStackSize();
        long withdrawn = plugin.getDatabaseManager().withdrawFromBank(player.getUniqueId(), clicked.getType(), maxStack);

        if (withdrawn <= 0) {
            player.sendMessage("§c[TaxPlugin] Non hai più nulla di questo materiale nella banca.");
            new BankGUI(plugin).open(player);
            return;
        }

        ItemStack toGive = clicked.clone();
        toGive.setAmount((int) withdrawn);

        var leftover = player.getInventory().addItem(toGive);
        leftover.values().forEach(extra ->
                player.getWorld().dropItemNaturally(player.getLocation(), extra));

        player.sendMessage("§a[TaxPlugin] Hai ritirato " + withdrawn + "x " + clicked.getType().name() + " dalla banca.");

        new BankGUI(plugin).open(player);
    }
}
