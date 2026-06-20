package com.server.taxplugin.storage;

import com.server.taxplugin.models.BankEntry;
import com.server.taxplugin.models.TrackedChest;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "taxplugin.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Impossibile connettersi al database SQLite", e);
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Errore durante la chiusura del database", e);
            }
        }
    }

    private void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS tracked_chests (
                    world TEXT NOT NULL,
                    x INTEGER NOT NULL,
                    y INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    owner TEXT NOT NULL,
                    PRIMARY KEY (world, x, y, z)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS bank_entries (
                    owner TEXT NOT NULL,
                    material TEXT NOT NULL,
                    amount INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (owner, material)
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS plugin_state (
                    key TEXT PRIMARY KEY,
                    value TEXT
                )
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS player_tax_overrides (
                    owner TEXT PRIMARY KEY,
                    percentage REAL NOT NULL
                )
            """);
        }
    }

    public void addTrackedChest(TrackedChest chest) {
        String sql = "INSERT OR REPLACE INTO tracked_chests (world, x, y, z, owner) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, chest.getWorld());
            ps.setInt(2, chest.getX());
            ps.setInt(3, chest.getY());
            ps.setInt(4, chest.getZ());
            ps.setString(5, chest.getOwner().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore salvataggio chest tracciata", e);
        }
    }

    public void removeTrackedChest(String world, int x, int y, int z) {
        String sql = "DELETE FROM tracked_chests WHERE world = ? AND x = ? AND y = ? AND z = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, world);
            ps.setInt(2, x);
            ps.setInt(3, y);
            ps.setInt(4, z);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore rimozione chest tracciata", e);
        }
    }

    public List<TrackedChest> getAllTrackedChests() {
        List<TrackedChest> result = new ArrayList<>();
        String sql = "SELECT world, x, y, z, owner FROM tracked_chests";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                UUID owner = UUID.fromString(rs.getString("owner"));
                result.add(new TrackedChest(owner, rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore lettura chest tracciate", e);
        }
        return result;
    }

    public List<TrackedChest> getChestsOwnedBy(UUID owner) {
        List<TrackedChest> result = new ArrayList<>();
        String sql = "SELECT world, x, y, z, owner FROM tracked_chests WHERE owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new TrackedChest(owner, rs.getString("world"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore lettura chest del player", e);
        }
        return result;
    }

    public void addToBank(UUID owner, Material material, long amount) {
        if (amount <= 0) return;
        String sql = """
            INSERT INTO bank_entries (owner, material, amount) VALUES (?, ?, ?)
            ON CONFLICT(owner, material) DO UPDATE SET amount = amount + excluded.amount
        """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setString(2, material.name());
            ps.setLong(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore aggiunta item alla banca virtuale", e);
        }
    }

    public long withdrawFromBank(UUID owner, Material material, long amount) {
        String selectSql = "SELECT amount FROM bank_entries WHERE owner = ? AND material = ?";
        long current = 0;
        try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
            ps.setString(1, owner.toString());
            ps.setString(2, material.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    current = rs.getLong("amount");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore lettura banca virtuale", e);
            return 0;
        }

        long actual = Math.min(amount, current);
        if (actual <= 0) return 0;

        String updateSql = "UPDATE bank_entries SET amount = amount - ? WHERE owner = ? AND material = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setLong(1, actual);
            ps.setString(2, owner.toString());
            ps.setString(3, material.name());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore aggiornamento banca virtuale", e);
            return 0;
        }
        return actual;
    }

    public List<BankEntry> getBankEntries(UUID owner) {
        List<BankEntry> result = new ArrayList<>();
        String sql = "SELECT material, amount FROM bank_entries WHERE owner = ? AND amount > 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Material mat = Material.matchMaterial(rs.getString("material"));
                    if (mat != null) {
                        result.add(new BankEntry(owner, mat, rs.getLong("amount")));
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore lettura entries banca virtuale", e);
        }
        return result;
    }

    public void setState(String key, String value) {
        String sql = "INSERT INTO plugin_state (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore salvataggio stato plugin", e);
        }
    }

    public String getState(String key) {
        String sql = "SELECT value FROM plugin_state WHERE key = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore lettura stato plugin", e);
        }
        return null;
    }

    public void setPlayerTaxOverride(UUID owner, double percentage) {
        String sql = "INSERT INTO player_tax_overrides (owner, percentage) VALUES (?, ?) " +
                "ON CONFLICT(owner) DO UPDATE SET percentage = excluded.percentage";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setDouble(2, percentage);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore salvataggio override percentuale player", e);
        }
    }

    public void removePlayerTaxOverride(UUID owner) {
        String sql = "DELETE FROM player_tax_overrides WHERE owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore rimozione override percentuale player", e);
        }
    }

    public Double getPlayerTaxOverride(UUID owner) {
        String sql = "SELECT percentage FROM player_tax_overrides WHERE owner = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("percentage");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Errore lettura override percentuale player", e);
        }
        return null;
    }
}
