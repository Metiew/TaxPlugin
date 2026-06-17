package com.server.taxplugin.models;

import java.util.UUID;

public class TrackedChest {

    private final UUID owner;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public TrackedChest(UUID owner, String world, int x, int y, int z) {
        this.owner = owner;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String positionKey() {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
