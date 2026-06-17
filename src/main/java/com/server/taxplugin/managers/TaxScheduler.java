package com.server.taxplugin.managers;

import com.server.taxplugin.TaxPlugin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TaxScheduler {

    private final TaxPlugin plugin;
    private final TaxManager taxManager;
    private int taskId = -1;

    public TaxScheduler(TaxPlugin plugin, TaxManager taxManager) {
        this.plugin = plugin;
        this.taxManager = taxManager;
    }

    public void start() {
        stop();
        checkAndRecoverMissedRun();
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(
                plugin, this::tick, 20L * 60, 20L * 60);
    }

    public void stop() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void tick() {
        if (!plugin.getConfig().getBoolean("enabled", false)) return;

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalTime targetTime = parseTaxTime();
        LocalDate lastRunDate = getLastRunDate();

        boolean alreadyRanToday = lastRunDate != null && lastRunDate.isEqual(now.toLocalDate());
        boolean pastTargetTime = !now.toLocalTime().isBefore(targetTime);

        if (pastTargetTime && !alreadyRanToday) {
            executeAndRecordRun();
        }
    }

    private void checkAndRecoverMissedRun() {
        if (!plugin.getConfig().getBoolean("enabled", false)) return;

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalTime targetTime = parseTaxTime();
        LocalDate lastRunDate = getLastRunDate();

        boolean alreadyRanToday = lastRunDate != null && lastRunDate.isEqual(now.toLocalDate());
        boolean missedToday = !now.toLocalTime().isBefore(targetTime) && !alreadyRanToday;

        boolean missedPreviousDays = lastRunDate != null && lastRunDate.isBefore(now.toLocalDate().minusDays(1));

        if (missedToday || missedPreviousDays) {
            plugin.getLogger().info("Tassazione di recupero in corso (orario saltato durante l'inattività del server)...");
            executeAndRecordRun();
        }
    }

    private void executeAndRecordRun() {
        taxManager.runTaxation();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        long epochMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        plugin.getConfig().set("last-tax-run", epochMillis);
        plugin.saveConfig();
        plugin.getDatabaseManager().setState("last-tax-run", String.valueOf(epochMillis));
    }

    private LocalDate getLastRunDate() {
        long millis = plugin.getConfig().getLong("last-tax-run", 0);
        if (millis <= 0) {
            String stored = plugin.getDatabaseManager().getState("last-tax-run");
            if (stored != null) {
                try {
                    millis = Long.parseLong(stored);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (millis <= 0) return null;
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault()).toLocalDate();
    }

    private LocalTime parseTaxTime() {
        String raw = plugin.getConfig().getString("tax-time", "20:00");
        try {
            return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            plugin.getLogger().warning("Formato tax-time invalido ('" + raw + "'), uso il default 20:00");
            return LocalTime.of(20, 0);
        }
    }
}
