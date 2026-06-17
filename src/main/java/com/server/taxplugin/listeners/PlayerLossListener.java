package com.server.taxplugin.listeners;

import com.server.taxplugin.TaxPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerLossListener implements Listener {

    private final TaxPlugin plugin;

    private final Map<UUID, List<ItemStack>> pendingRestoration = new ConcurrentHashMap<>();

    private final Map<UUID, DropWindow> dropTracking = new ConcurrentHashMap<>();

    public PlayerLossListener(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> drops = new ArrayList<>(event.getDrops());
        event.getDrops().clear();
        pendingRestoration.put(player.getUniqueId(), drops);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        List<ItemStack> saved = pendingRestoration.remove(player.getUniqueId());
        if (saved == null || saved.isEmpty()) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (ItemStack item : saved) {
                if (item == null) continue;
                var leftover = player.getInventory().addItem(item);
                leftover.values().forEach(extra ->
                        player.getWorld().dropItemNaturally(player.getLocation(), extra));
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getConfig().getBoolean("enabled", false)) return;

        Player player = event.getPlayer();
        int amount = event.getItemDrop().getItemStack().getAmount();
        long now = System.currentTimeMillis();

        int windowMinutes = plugin.getConfig().getInt("anti-evasion-window-minutes", 10);
        int threshold = plugin.getConfig().getInt("anti-evasion-threshold", 32);
        long windowMillis = windowMinutes * 60_000L;

        DropWindow window = dropTracking.computeIfAbsent(player.getUniqueId(), k -> new DropWindow(now, 0));

        if (now - window.windowStart > windowMillis) {
            window.windowStart = now;
            window.totalDropped = 0;
            window.alreadyNotified = false;
        }

        window.totalDropped += amount;

        if (!window.alreadyNotified && window.totalDropped >= threshold) {
            window.alreadyNotified = true;
            String message = "§e[TaxPlugin] §c" + player.getName()
                    + " ha droppato " + window.totalDropped
                    + " item negli ultimi " + windowMinutes + " minuti (possibile evasione fiscale).";
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("taxplugin.admin"))
                    .forEach(p -> p.sendMessage(message));
            plugin.getLogger().warning(message);
        }
    }

    private static class DropWindow {
        long windowStart;
        int totalDropped;
        boolean alreadyNotified;

        DropWindow(long windowStart, int totalDropped) {
            this.windowStart = windowStart;
            this.totalDropped = totalDropped;
        }
    }
}
