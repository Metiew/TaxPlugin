package com.server.taxplugin.commands;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.gui.BankGUI;
import com.server.taxplugin.gui.PercentageGUI;
import com.server.taxplugin.gui.WeightsGUI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TaxCommand implements CommandExecutor {

    private final TaxPlugin plugin;

    public TaxCommand(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUso: /tax <enable|disable|run|setpercent <player>|weights|bank|status>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "enable" -> handleEnable(sender);
            case "disable" -> handleDisable(sender);
            case "run" -> handleRun(sender);
            case "setpercent" -> handleSetPercent(sender, args);
            case "weights" -> handleWeights(sender);
            case "bank" -> handleBank(sender);
            case "status" -> handleStatus(sender);
            default -> sender.sendMessage("§cSottocomando non riconosciuto. Usa /tax per vedere la lista.");
        }

        return true;
    }

    private boolean requireAdmin(CommandSender sender) {
        if (!sender.hasPermission("taxplugin.admin")) {
            sender.sendMessage("§cNon hai il permesso per usare questo comando.");
            return false;
        }
        return true;
    }

    private void handleEnable(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        plugin.getConfig().set("enabled", true);
        plugin.saveConfig();
        plugin.getTaxScheduler().start();
        sender.sendMessage("§a[TaxPlugin] Plugin attivato. La tassazione partirà secondo l'orario configurato (tax-time).");
    }

    private void handleDisable(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        plugin.getConfig().set("enabled", false);
        plugin.saveConfig();
        plugin.getTaxScheduler().stop();
        sender.sendMessage("§c[TaxPlugin] Plugin disattivato. Nessuna tassazione verrà più eseguita finché non lo riattivi.");
    }

    private void handleRun(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        sender.sendMessage("§e[TaxPlugin] Esecuzione tassazione manuale in corso...");
        plugin.getTaxManager().runTaxation();
    }

    private void handleSetPercent(CommandSender sender, String[] args) {
        if (!requireAdmin(sender)) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cQuesto comando richiede di essere in gioco (apre una GUI).");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUso corretto: /tax setpercent <nome giocatore>");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§c[TaxPlugin] Nessun giocatore trovato con quel nome.");
            return;
        }

        new PercentageGUI(plugin, target.getUniqueId(), targetName).open(player);
    }

    private void handleWeights(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cQuesto comando richiede di essere in gioco (apre una GUI).");
            return;
        }
        new WeightsGUI(plugin).open(player);
    }

    private void handleBank(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cQuesto comando richiede di essere in gioco.");
            return;
        }
        if (!player.hasPermission("taxplugin.bank")) {
            sender.sendMessage("§cNon hai il permesso per accedere alla banca virtuale.");
            return;
        }
        new BankGUI(plugin).open(player);
    }

    private void handleStatus(CommandSender sender) {
        if (!requireAdmin(sender)) return;
        boolean enabled = plugin.getConfig().getBoolean("enabled", false);
        String time = plugin.getConfig().getString("tax-time", "20:00");
        double defaultPercent = plugin.getConfig().getDouble("default-tax-percentage", 10.0);

        sender.sendMessage("§6=== TaxPlugin Status ===");
        sender.sendMessage("§7Attivo: " + (enabled ? "§aSì" : "§cNo"));
        sender.sendMessage("§7Orario tassazione: §e" + time);
        sender.sendMessage("§7Percentuale di default (per chi non ha override): §e" + defaultPercent + "%");
        sender.sendMessage("§7Usa /tax setpercent <player> per impostare la tassa di un singolo giocatore.");
    }
}
