package me.unprankable.blockparty.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.unprankable.blockparty.BlockParty;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Info {
    private static final Gson GSON = new GsonBuilder().create();

    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty info <region_name>");
            BlockParty.getInstance().debugLog("Info command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Info command: Region file not found for " + regionName);
            return false;
        }

        try (FileReader reader = new FileReader(regionFile)) {
            Map<String, Object> regionData = GSON.fromJson(reader, Map.class);

            if (regionData == null) {
                sender.sendMessage(ChatColor.RED + "Failed to read region data.");
                BlockParty.getInstance().errorLog("Failed to parse region file: " + regionFile.getAbsolutePath());
                return false;
            }

            // Display region information
            sender.sendMessage(ChatColor.GOLD + "====== Region Info: " + regionName + " ======");
            sender.sendMessage(ChatColor.YELLOW + "Name: " + ChatColor.RESET + regionData.getOrDefault("name", "N/A"));
            sender.sendMessage(ChatColor.YELLOW + "World: " + ChatColor.RESET + regionData.getOrDefault("world", "N/A"));

            // Display positions
            Object pos1 = regionData.get("pos1");
            Object pos2 = regionData.get("pos2");
            if (pos1 != null && pos2 != null) {
                sender.sendMessage(ChatColor.YELLOW + "Position 1: " + ChatColor.RESET + pos1.toString());
                sender.sendMessage(ChatColor.YELLOW + "Position 2: " + ChatColor.RESET + pos2.toString());
            }

            // Display blocks
            Object blocksObj = regionData.get("blocks");
            if (blocksObj != null) {
                if (blocksObj instanceof java.util.List) {
                    java.util.List<?> blocks = (java.util.List<?>) blocksObj;
                    if (blocks.isEmpty()) {
                        sender.sendMessage(ChatColor.YELLOW + "Blocks: " + ChatColor.RESET + "None");
                    } else {
                        sender.sendMessage(ChatColor.YELLOW + "Blocks: " + ChatColor.RESET + String.join(", ", blocks.stream().map(Object::toString).toArray(String[]::new)));
                    }
                }
            }

            // Display minPlayers
            Object minPlayersObj = regionData.get("minPlayers");
            if (minPlayersObj != null) {
                sender.sendMessage(ChatColor.YELLOW + "Minimum Players: " + ChatColor.RESET + minPlayersObj.toString());
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Minimum Players: " + ChatColor.RESET + "2 (default)");
            }

            // Display numRounds
            Object numRoundsObj = regionData.get("numRounds");
            if (numRoundsObj != null) {
                int numRounds = ((Number) numRoundsObj).intValue();
                if (numRounds > 0) {
                    sender.sendMessage(ChatColor.YELLOW + "Maximum Rounds: " + ChatColor.RESET + numRounds);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Maximum Rounds: " + ChatColor.RESET + "Unlimited");
                }
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Maximum Rounds: " + ChatColor.RESET + "Unlimited");
            }

            sender.sendMessage(ChatColor.GOLD + "==============================");
            BlockParty.getInstance().debugLog("Info command executed for region: " + regionName + " by " + sender.getName());
            return true;

        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed to read region file.");
            BlockParty.getInstance().errorLog("Failed to read region file for " + regionName + ": " + e.getMessage());
            return false;
        }
    }
}
