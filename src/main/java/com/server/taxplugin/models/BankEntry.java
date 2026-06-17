package com.server.taxplugin.models;

import org.bukkit.Material;

import java.util.UUID;

public class BankEntry {

    private final UUID owner;
    private final Material material;
    private long amount;

    public BankEntry(UUID owner, Material material, long amount) {
        this.owner = owner;
        this.material = material;
        this.amount = amount;
    }

    public UUID getOwner() {
        return owner;
    }

    public Material getMaterial() {
        return material;
    }

    public long getAmount() {
        return amount;
    }

    public void add(long quantity) {
        this.amount += quantity;
    }

    public long subtract(long quantity) {
        long actual = Math.min(quantity, amount);
        amount -= actual;
        return actual;
    }
}
