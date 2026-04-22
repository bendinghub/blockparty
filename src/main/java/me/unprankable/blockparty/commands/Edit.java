package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;

public class Edit {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty edit <region_name> <option> <value>");
            BlockParty.getInstance().debugLog("Edit command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        String option = args[2];
        String value = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));

        if (RegionManager.updateRegionOption(regionName, option, value)) {
            sender.sendMessage(ChatColor.GREEN + "Region '" + regionName + "' updated successfully.");
            sender.sendMessage(ChatColor.YELLOW + "Updated option: " + ChatColor.RESET + option);
            BlockParty.getInstance().debugLog("Region edited: " + regionName + " by " + sender.getName());
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to update region '" + regionName + "'.");
            BlockParty.getInstance().errorLog("Failed to update region: " + regionName);
            return false;
        }
    }

    public static java.util.List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], RegionManager.getRegionNames(), new ArrayList<>());
        }
        if (args.length == 3) {
            return StringUtil.copyPartialMatches(args[2], java.util.List.of("minPlayers", "blocks", "name", "preservePattern"), new ArrayList<>());
        }
        if (args.length == 4 && args[2].equalsIgnoreCase("preservePattern")) {
            return StringUtil.copyPartialMatches(args[3], java.util.List.of("true", "false"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
