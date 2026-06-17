package com.server.taxplugin.listeners;

import com.server.taxplugin.models.TrackedChest;
import com.server.taxplugin.storage.DatabaseManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ChestTrackingListener implements Listener {

    private final DatabaseManager db;

    public ChestTrackingListener(DatabaseManager db) {
        this.db = db;
    }

    private boolean isChestType(Material material) {
        return material == Material.CHEST || material == Material.TRAPPED_CHEST;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!isChestType(block.getType())) return;

        TrackedChest chest = new TrackedChest(
                event.getPlayer().getUniqueId(),
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );
        db.addTrackedChest(chest);
    }

    @EventHandler(ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isChestType(block.getType())) return;

        db.removeTrackedChest(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }
}
