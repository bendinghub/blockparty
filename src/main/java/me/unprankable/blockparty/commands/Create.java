package me.unprankable.blockparty.commands;

import com.sk89q.worldedit.regions.Region;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.hooks.WorldEditHook;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;

public class Create {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty create <region_name> [minPlayers]");
            return false;
        }
        //get worldedit selection
        Region region = WorldEditHook.getPlayerSelection(((Player) sender).getPlayer());
        if (region == null){
            return false;
        }

        int minPlayers = 2; // default

        if (args.length > 2) {
            try {
                minPlayers = Integer.parseInt(args[2]);
                BlockParty.getInstance().debugLog("Create command: MinPlayers specified: " + minPlayers);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "MinPlayers must be a number.");
                return false;
            }
        }

        if (RegionManager.createRegion(args[1], region, minPlayers)) {
            sender.sendMessage(ChatColor.GREEN + "Blockparty region created");
            sender.sendMessage(ChatColor.GREEN + "Blocks: auto-generated from selected region");
            sender.sendMessage(ChatColor.GREEN + "Minimum Players: " + minPlayers);
            BlockParty.getInstance().debugLog("Region created: " + args[1] + " by " + sender.getName());
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create region.");
            return false;
        }
    }

    public static java.util.List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
