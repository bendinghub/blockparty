package me.unprankable.blockparty.commands;

import com.sk89q.worldedit.regions.Region;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.hooks.WorldEditHook;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Create {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty create <region_name> [block1,block2,...] [minPlayers] [numRounds]");
            return false;
        }
        //get worldedit selection
        Region region = WorldEditHook.getPlayerSelection(((Player) sender).getPlayer());
        if (region == null){
            return false;
        }
        
        // Parse blocks from arguments (format: /blockparty create <name> [block1,block2,block3...] [minPlayers] [numRounds])
        List<String> blocks = new ArrayList<>();
        int minPlayers = 2; // default
        int numRounds = 0; // default (unlimited)
        
        if (args.length > 2) {
            blocks = Arrays.asList(args[2].split(","));
            BlockParty.getInstance().debugLog("Create command: Blocks specified: " + blocks);
        }
        
        if (args.length > 3) {
            try {
                minPlayers = Integer.parseInt(args[3]);
                BlockParty.getInstance().debugLog("Create command: MinPlayers specified: " + minPlayers);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "MinPlayers must be a number.");
                return false;
            }
        }
        
        if (args.length > 4) {
            try {
                numRounds = Integer.parseInt(args[4]);
                BlockParty.getInstance().debugLog("Create command: NumRounds specified: " + numRounds);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "NumRounds must be a number.");
                return false;
            }
        }
        
        if (RegionManager.createRegion(args[1], region, blocks, minPlayers, numRounds)) {
            sender.sendMessage(ChatColor.GREEN + "Blockparty region created");
            if (!blocks.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "Blocks: " + String.join(", ", blocks));
            }
            sender.sendMessage(ChatColor.GREEN + "Minimum Players: " + minPlayers);
            if (numRounds > 0) {
                sender.sendMessage(ChatColor.GREEN + "Maximum Rounds: " + numRounds);
            }
            BlockParty.getInstance().debugLog("Region created: " + args[1] + " by " + sender.getName());
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create region.");
            return false;
        }
    }
}
