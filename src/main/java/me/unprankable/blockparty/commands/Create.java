package me.unprankable.blockparty.commands;

import com.sk89q.worldedit.regions.Region;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.hooks.WorldEditHook;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;

public class Create {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty create <region_name> [minPlayers] [preservePattern]");
            return false;
        }
        if (args.length > 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty create <region_name> [minPlayers] [preservePattern]");
            return false;
        }
        //get worldedit selection
        Region region = WorldEditHook.getPlayerSelection(((Player) sender).getPlayer());
        if (region == null){
            return false;
        }

        int minPlayers = 2; // default
        boolean preservePattern = false;

        if (args.length > 2) {
            Boolean parsedBoolean = parseBooleanArg(args[2]);
            if (parsedBoolean != null) {
                preservePattern = parsedBoolean;
                BlockParty.getInstance().debugLog("Create command: PreservePattern specified: " + preservePattern);
            } else {
                try {
                    minPlayers = Integer.parseInt(args[2]);
                    BlockParty.getInstance().debugLog("Create command: MinPlayers specified: " + minPlayers);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Third argument must be minPlayers (number) or preservePattern (true/false).");
                    return false;
                }
            }
        }

        if (args.length > 3) {
            Boolean parsedBoolean = parseBooleanArg(args[3]);
            if (parsedBoolean == null) {
                sender.sendMessage(ChatColor.RED + "preservePattern must be true or false.");
                return false;
            }
            preservePattern = parsedBoolean;
            BlockParty.getInstance().debugLog("Create command: PreservePattern specified: " + preservePattern);
        }

        if (RegionManager.createRegion(args[1], region, minPlayers, preservePattern)) {
            sender.sendMessage(ChatColor.GREEN + "Blockparty region created");
            sender.sendMessage(ChatColor.GREEN + "Blocks: auto-generated from selected region");
            sender.sendMessage(ChatColor.GREEN + "Minimum Players: " + minPlayers);
            sender.sendMessage(ChatColor.GREEN + "Preserve Pattern: " + preservePattern);
            BlockParty.getInstance().debugLog("Region created: " + args[1] + " by " + sender.getName());
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create region.");
            return false;
        }
    }

    private static Boolean parseBooleanArg(String value) {
        String normalized = value.toLowerCase();
        return switch (normalized) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> null;
        };
    }

    public static java.util.List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 3) {
            return StringUtil.copyPartialMatches(args[2], java.util.List.of("2", "true", "false"), new ArrayList<>());
        }
        if (args.length == 4) {
            return StringUtil.copyPartialMatches(args[3], java.util.List.of("true", "false"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
