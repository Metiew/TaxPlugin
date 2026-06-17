package com.server.taxplugin;

import com.server.taxplugin.commands.TaxCommand;
import com.server.taxplugin.listeners.BankGUIListener;
import com.server.taxplugin.listeners.ChestTrackingListener;
import com.server.taxplugin.listeners.PercentageGUIListener;
import com.server.taxplugin.listeners.PlayerLossListener;
import com.server.taxplugin.listeners.WeightsGUIListener;
import com.server.taxplugin.managers.TaxManager;
import com.server.taxplugin.managers.TaxScheduler;
import com.server.taxplugin.storage.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

public class TaxPlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private TaxManager taxManager;
    private TaxScheduler taxScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        taxManager = new TaxManager(this, databaseManager);
        taxScheduler = new TaxScheduler(this, taxManager);

        registerListeners();
        registerCommands();

        taxScheduler.start();

        getLogger().info("TaxPlugin caricato correttamente.");
    }

    @Override
    public void onDisable() {
        if (taxScheduler != null) {
            taxScheduler.stop();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("TaxPlugin disattivato.");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChestTrackingListener(databaseManager), this);
        getServer().getPluginManager().registerEvents(new PlayerLossListener(this), this);
        getServer().getPluginManager().registerEvents(new BankGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new WeightsGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new PercentageGUIListener(this), this);
    }

    private void registerCommands() {
        var command = getCommand("tax");
        if (command != null) {
            command.setExecutor(new TaxCommand(this));
        } else {
            getLogger().severe("Comando 'tax' non trovato nel plugin.yml! Controlla la configurazione.");
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public TaxManager getTaxManager() {
        return taxManager;
    }

    public TaxScheduler getTaxScheduler() {
        return taxScheduler;
    }
}
