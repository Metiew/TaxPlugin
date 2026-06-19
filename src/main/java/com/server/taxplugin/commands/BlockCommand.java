package com.server.taxplugin.commands;

import com.server.taxplugin.TaxPlugin;
import com.server.taxplugin.gui.BlockPlayerSelectGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BlockCommand implements CommandExecutor {

    private final TaxPlugin plugin;

    public BlockCommand(TaxPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("taxplugin.admin")) {
            sender.sendMessage("§cNon hai il permesso per usare questo comando.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cQuesto comando richiede di essere in gioco (apre una GUI).");
            return true;
        }

        new BlockPlayerSelectGUI(plugin).open(player);
        return true;
    }
}
