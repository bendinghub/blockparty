package me.unprankable.blockparty.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.GameManager;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Start {
    private static final Gson GSON = new GsonBuilder().create();

    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty start <region_name> [force]");
            BlockParty.getInstance().debugLog("Start command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Start command: Region file not found for " + regionName);
            return false;
        }

        try (FileReader reader = new FileReader(regionFile)) {
            Map<String, Object> regionData = GSON.fromJson(reader, Map.class);

            if (regionData == null) {
                sender.sendMessage(ChatColor.RED + "Failed to read region data.");
                BlockParty.getInstance().errorLog("Failed to parse region file: " + regionFile.getAbsolutePath());
                return false;
            }

            // Get minPlayers from region (default 2)
            int minPlayers = 2;
            Object minPlayersObj = regionData.get("minPlayers");
            if (minPlayersObj != null) {
                minPlayers = ((Number) minPlayersObj).intValue();
            }

            // Get current player count in region
            int currentPlayers = GameManager.getPlayerCountInRegion(regionName);
            List<String> playersInRegion = GameManager.getPlayerNamesInRegion(regionName);

            // Check if this is a force start
            boolean forceStart = args.length > 2 && args[2].equalsIgnoreCase("force");

            if (!forceStart && currentPlayers < minPlayers) {
                sender.sendMessage(ChatColor.RED + "Not enough players to start. Required: " + minPlayers + ", Current: " + currentPlayers);
                sender.sendMessage(ChatColor.YELLOW + "Use /blockparty start " + regionName + " force to force start.");
                BlockParty.getInstance().debugLog("Start command: Not enough players for " + regionName + " (required: " + minPlayers + ", current: " + currentPlayers + ")");
                return false;
            }

            // Check if a game session is already active
            if (GameManager.hasActiveSession(regionName)) {
                sender.sendMessage(ChatColor.RED + "A game is already active in this region.");
                return false;
            }

            try {
                GameManager.startGameSession(regionName, forceStart);
                if (forceStart) {
                    sender.sendMessage(ChatColor.GREEN + "BlockParty game force-started for region '" + regionName + "'.");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "BlockParty game waiting phase started for region '" + regionName + "'.");
                }
                sender.sendMessage(ChatColor.YELLOW + "Players: " + String.join(", ", playersInRegion));
                BlockParty.getInstance().debugLog("BlockParty game started for region: " + regionName + " (players: " + currentPlayers + ") by " + sender.getName());
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Failed to start game: " + e.getMessage());
                BlockParty.getInstance().errorLog("Failed to start game session for " + regionName + ": " + e.getMessage());
                return false;
            }

            return true;

        } catch (IOException e) {
            sender.sendMessage(ChatColor.RED + "Failed to read region file.");
            BlockParty.getInstance().errorLog("Failed to read region file for " + regionName + ": " + e.getMessage());
            return false;
        }
    }

    public static List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], RegionManager.getRegionNames(), new ArrayList<>());
        }
        if (args.length == 3) {
            return StringUtil.copyPartialMatches(args[2], Collections.singletonList("force"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
