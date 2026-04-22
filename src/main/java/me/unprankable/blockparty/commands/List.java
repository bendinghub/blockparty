package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class List {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionsDirFile = regionsDir.toFile();

        if (!regionsDirFile.exists()) {
            sender.sendMessage(ChatColor.YELLOW + "No regions directory found. Create a region first.");
            BlockParty.getInstance().debugLog("List command executed by " + sender.getName() + " - no regions directory");
            return true;
        }

        File[] regionFiles = regionsDirFile.listFiles((dir, name) -> name.endsWith(".json"));

        if (regionFiles == null || regionFiles.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No BlockParty regions found.");
            BlockParty.getInstance().debugLog("List command executed by " + sender.getName() + " - no regions found");
            return true;
        }

        // Create a list of region names and sort them
        java.util.List<String> regionNames = new ArrayList<>();
        for (File file : regionFiles) {
            String name = file.getName();
            // Remove the .json extension
            regionNames.add(name.substring(0, name.length() - 5));
        }
        Collections.sort(regionNames);

        // Display the regions
        sender.sendMessage(ChatColor.GOLD + "====== BlockParty Regions (" + regionNames.size() + ") ======");
        for (String regionName : regionNames) {
            sender.sendMessage(ChatColor.YELLOW + "• " + ChatColor.RESET + regionName);
        }
        sender.sendMessage(ChatColor.GOLD + "===================================");

        BlockParty.getInstance().debugLog("List command executed by " + sender.getName() + " - found " + regionNames.size() + " regions");
        return true;
    }

    public static java.util.List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
