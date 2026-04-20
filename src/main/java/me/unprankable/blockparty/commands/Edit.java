package me.unprankable.blockparty.commands;

import com.sk89q.worldedit.regions.Region;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.hooks.WorldEditHook;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Edit {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty edit <region_name> [block1,block2,block3...] [minPlayers] [numRounds]");
            BlockParty.getInstance().debugLog("Edit command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Edit command: Region file not found for " + regionName);
            return false;
        }

        // Get the player's WorldEdit selection
        Region region = WorldEditHook.getPlayerSelection(((Player) sender).getPlayer());
        if (region == null){
            BlockParty.getInstance().debugLog("Edit command: Failed to get selection for " + regionName);
            return false;
        }

        // Parse blocks from arguments if provided
        List<String> blocks = new ArrayList<>();
        if (args.length > 2) {
            blocks = Arrays.asList(args[2].split(","));
            BlockParty.getInstance().debugLog("Edit command: Blocks specified: " + blocks);
        }

        // Parse minPlayers from arguments if provided
        int minPlayers = 2; // default
        if (args.length > 3) {
            try {
                minPlayers = Integer.parseInt(args[3]);
                BlockParty.getInstance().debugLog("Edit command: MinPlayers specified: " + minPlayers);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "MinPlayers must be a number.");
                return false;
            }
        }

        // Parse numRounds from arguments if provided
        int numRounds = 0; // default (unlimited)
        if (args.length > 4) {
            try {
                numRounds = Integer.parseInt(args[4]);
                BlockParty.getInstance().debugLog("Edit command: NumRounds specified: " + numRounds);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "NumRounds must be a number.");
                return false;
            }
        }

        // Update the region with the new selection
        if (RegionManager.createRegion(regionName, region, blocks, minPlayers, numRounds)) {
            sender.sendMessage(ChatColor.GREEN + "Region '" + regionName + "' updated successfully.");
            if (!blocks.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "Blocks: " + String.join(", ", blocks));
            }
            sender.sendMessage(ChatColor.GREEN + "Minimum Players: " + minPlayers);
            if (numRounds > 0) {
                sender.sendMessage(ChatColor.GREEN + "Maximum Rounds: " + numRounds);
            }
            BlockParty.getInstance().debugLog("Region edited: " + regionName + " by " + sender.getName());
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to update region '" + regionName + "'.");
            BlockParty.getInstance().errorLog("Failed to update region: " + regionName);
            return false;
        }
    }
}
