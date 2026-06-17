package com.server.taxplugin.managers;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.models.TrackedChest;
import com.server.taxplugin.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TaxManager {

    private final TaxPlugin plugin;
    private final DatabaseManager db;

    public TaxManager(TaxPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public void runTaxation() {
        double percentage = plugin.getConfig().getDouble("tax-percentage", 10.0);
        Map<String, Integer> weights = loadWeights();
        Set<Material> exempt = loadExempt();

        Set<UUID> targets = new java.util.HashSet<>();
        Bukkit.getOnlinePlayers().forEach(p -> targets.add(p.getUniqueId()));
        db.getAllTrackedChests().forEach(c -> targets.add(c.getOwner()));

        int taxedCount = 0;
        for (UUID target : targets) {
            boolean taxed = taxPlayer(target, percentage, weights, exempt);
            if (taxed) taxedCount++;
        }

        plugin.getLogger().info("Tassazione completata: " + taxedCount + " giocatori coinvolti.");
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("taxplugin.admin"))
                .forEach(p -> p.sendMessage("§e[TaxPlugin] §aTassazione giornaliera completata per " + taxedCount + " giocatori."));

        Bukkit.getOnlinePlayers().forEach(p ->
                p.sendMessage("§e[TaxPlugin] §7È stata applicata la tassa giornaliera. Controlla la tua banca virtuale con /tax bank."));
    }

    private boolean taxPlayer(UUID target, double percentage, Map<String, Integer> weights, Set<Material> exempt) {
        List<ItemSource> sources = new ArrayList<>();

        Player online = Bukkit.getPlayer(target);
        if (online != null && online.isOnline()) {
            sources.add(new ItemSource(online.getInventory()));
        }

        List<TrackedChest> chests = db.getChestsOwnedBy(target);
        for (TrackedChest tc : chests) {
            World world = Bukkit.getWorld(tc.getWorld());
            if (world == null) continue;
            Block block = world.getBlockAt(tc.getX(), tc.getY(), tc.getZ());
            BlockState state = block.getState();
            if (state instanceof Chest chestState) {
                sources.add(new ItemSource(chestState.getBlockInventory()));
            }
        }

        if (sources.isEmpty()) return false;

        double totalValue = 0;
        for (ItemSource source : sources) {
            for (ItemStack item : source.inventory.getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                if (isFood(item.getType()) || exempt.contains(item.getType())) continue;
                int weight = weights.getOrDefault(item.getType().name(), 0);
                totalValue += (double) weight * item.getAmount();
            }
        }

        if (totalValue <= 0) return false;

        double amountDue = totalValue * (percentage / 100.0);
        if (amountDue <= 0) return false;

        List<WeightedStack> allStacks = new ArrayList<>();
        for (ItemSource source : sources) {
            ItemStack[] contents = source.inventory.getContents();
            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (item == null || item.getType() == Material.AIR) continue;
                if (isFood(item.getType()) || exempt.contains(item.getType())) continue;
                int weight = weights.getOrDefault(item.getType().name(), 0);
                if (weight <= 0) continue;
                allStacks.add(new WeightedStack(source.inventory, slot, item, weight));
            }
        }

        allStacks.sort((a, b) -> Integer.compare(b.weight, a.weight));

        Map<Material, Long> removedTotals = new HashMap<>();
        double remainingDue = amountDue;

        for (WeightedStack stack : allStacks) {
            if (remainingDue <= 0) break;

            int unitValue = stack.weight;
            int available = stack.item.getAmount();
            int unitsToRemove = (int) Math.min(available, Math.ceil(remainingDue / unitValue));
            if (unitsToRemove <= 0) continue;

            removedTotals.merge(stack.item.getType(), (long) unitsToRemove, Long::sum);
            remainingDue -= (double) unitsToRemove * unitValue;

            if (unitsToRemove >= available) {
                stack.inventory.setItem(stack.slot, null);
            } else {
                ItemStack reduced = stack.item.clone();
                reduced.setAmount(available - unitsToRemove);
                stack.inventory.setItem(stack.slot, reduced);
            }
        }

        if (removedTotals.isEmpty()) return false;

        removedTotals.forEach((material, qty) -> db.addToBank(target, material, qty));
        return true;
    }

    private boolean isFood(Material material) {
        return material.isEdible();
    }

    private Map<String, Integer> loadWeights() {
        Map<String, Integer> weights = new HashMap<>();
        var section = plugin.getConfig().getConfigurationSection("item-weights");
        if (section == null) return weights;
        for (String key : section.getKeys(false)) {
            weights.put(key, section.getInt(key));
        }
        return weights;
    }

    private Set<Material> loadExempt() {
        Set<Material> result = new java.util.HashSet<>();
        for (String name : plugin.getConfig().getStringList("exempt-materials")) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) result.add(mat);
        }
        return result;
    }

    private record ItemSource(Inventory inventory) {
    }

    private static class WeightedStack {
        final Inventory inventory;
        final int slot;
        final ItemStack item;
        final int weight;

        WeightedStack(Inventory inventory, int slot, ItemStack item, int weight) {
            this.inventory = inventory;
            this.slot = slot;
            this.item = item;
            this.weight = weight;
        }
    }
}
