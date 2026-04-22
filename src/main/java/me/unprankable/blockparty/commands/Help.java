package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class Help {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        sender.sendMessage(ChatColor.GOLD + "====== BlockParty Help ======");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty create <name> [minPlayers] [preservePattern]" + ChatColor.RESET + " - Create a new region and auto-generate block list from selection");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty delete <name>" + ChatColor.RESET + " - Delete an existing region");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty edit <name> <option> <value>" + ChatColor.RESET + " - Edit one region option at a time");
        sender.sendMessage(ChatColor.GRAY + "  Options: minPlayers, blocks, name, preservePattern");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty list" + ChatColor.RESET + " - List all BlockParty regions");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty info <name>" + ChatColor.RESET + " - View detailed information about a region");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty join <name>" + ChatColor.RESET + " - Join a BlockParty region");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty leave" + ChatColor.RESET + " - Leave the current BlockParty region");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty start <name>" + ChatColor.RESET + " - Start a BlockParty game");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty stop <name>" + ChatColor.RESET + " - Stop a BlockParty game");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty reload" + ChatColor.RESET + " - Reload config and stop all active games");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty stats <player>" + ChatColor.RESET + " - View player statistics");
        sender.sendMessage(ChatColor.YELLOW + "/blockparty help" + ChatColor.RESET + " - Show this help message");
        sender.sendMessage(ChatColor.GOLD + "============================");
        BlockParty.getInstance().debugLog("Help command executed by " + sender.getName());
        return true;
    }

    public static List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
