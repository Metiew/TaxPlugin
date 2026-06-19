package com.server.taxplugin.listeners;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.gui.BlockArmorGUI;
import com.server.taxplugin.gui.BlockPlayerSelectGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class BlockGUIListener implements Listener {

    private final TaxPlugin plugin;

    public BlockGUIListener(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();

        if (BlockPlayerSelectGUI.TITLE.equals(title)) {
            handlePlayerSelect(event);
            return;
        }

        if (title.startsWith(BlockArmorGUI.TITLE_PREFIX)) {
            handleArmorToggle(event, title);
        }
    }

    private void handlePlayerSelect(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!viewer.hasPermission("taxplugin.admin")) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta == null || meta.getOwningPlayer() == null) return;

        Player target = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());
        if (target == null || !target.isOnline()) {
            viewer.sendMessage("§c[TaxPlugin] Quel giocatore non è più online.");
            return;
        }

        new BlockArmorGUI(plugin).open(viewer, target);
    }

    private void handleArmorToggle(InventoryClickEvent event, String title) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        if (!viewer.hasPermission("taxplugin.admin")) return;

        if (!BlockArmorGUI.isArmorSlot(event.getRawSlot())) return;

        String targetName = title.substring(BlockArmorGUI.TITLE_PREFIX.length());
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            viewer.sendMessage("§c[TaxPlugin] Quel giocatore non è più online.");
            viewer.closeInventory();
            return;
        }

        BlockArmorGUI gui = new BlockArmorGUI(plugin);
        gui.toggleExemptBySlot(target, event.getRawSlot());

        gui.open(viewer, target);
    }
}
