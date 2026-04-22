package me.unprankable.blockparty.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.GameManager;
import me.unprankable.blockparty.managers.GameSession;
import me.unprankable.blockparty.managers.RegionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

public class Join {
    private static final Gson GSON = new GsonBuilder().create();

    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty join <region_name>");
            BlockParty.getInstance().debugLog("Join command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        int minPlayers = 2;

        // Check if region exists
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Join command: Region file not found for " + regionName);
            return false;
        }

        // Check if player is already in a region
        String currentRegion = GameManager.getPlayerRegion(player.getUniqueId());
        if (currentRegion != null) {
            sender.sendMessage(ChatColor.RED + "You are already in region '" + currentRegion + "'. Use /blockparty leave first.");
            BlockParty.getInstance().debugLog("Join command: Player " + player.getName() + " tried to join " + regionName + " while in " + currentRegion);
            return false;
        }

        // Teleport to midpoint of the region before joining
        try (FileReader reader = new FileReader(regionFile)) {
            Map<String, Object> regionData = GSON.fromJson(reader, Map.class);
            if (regionData == null) {
                sender.sendMessage(ChatColor.RED + "Failed to read region data.");
                BlockParty.getInstance().errorLog("Join command: Failed to parse region file for " + regionName);
                return false;
            }

            String worldName = (String) regionData.get("world");
            java.util.List<?> pos1 = (java.util.List<?>) regionData.get("pos1");
            java.util.List<?> pos2 = (java.util.List<?>) regionData.get("pos2");

            Object minPlayersObj = regionData.get("minPlayers");
            if (minPlayersObj instanceof Number) {
                minPlayers = ((Number) minPlayersObj).intValue();
            }

            if (worldName == null || pos1 == null || pos2 == null || pos1.size() < 3 || pos2.size() < 3) {
                sender.sendMessage(ChatColor.RED + "Region data is invalid.");
                BlockParty.getInstance().errorLog("Join command: Missing world/pos1/pos2 in region " + regionName);
                return false;
            }

            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "Region world is not loaded: " + worldName);
                BlockParty.getInstance().errorLog("Join command: World not found for region " + regionName + ": " + worldName);
                return false;
            }

            int pos1x = ((Number) pos1.get(0)).intValue();
            int pos1y = ((Number) pos1.get(1)).intValue();
            int pos1z = ((Number) pos1.get(2)).intValue();
            int pos2x = ((Number) pos2.get(0)).intValue();
            int pos2y = ((Number) pos2.get(1)).intValue();
            int pos2z = ((Number) pos2.get(2)).intValue();

            double midX = (pos1x + pos2x) / 2.0 + 0.5;
            double midY = (pos1y + pos2y) / 2.0 + 1.0;
            double midZ = (pos1z + pos2z) / 2.0 + 0.5;

            Location teleportLocation = new Location(world, midX, midY, midZ, player.getLocation().getYaw(), player.getLocation().getPitch());
            boolean teleported = player.teleport(teleportLocation);
            if (!teleported) {
                sender.sendMessage(ChatColor.RED + "Failed to teleport you to the region.");
                BlockParty.getInstance().errorLog("Join command: Teleport failed for " + player.getName() + " to region " + regionName);
                return false;
            }
        } catch (IOException | ClassCastException | NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Failed to load region coordinates.");
            BlockParty.getInstance().errorLog("Join command: Failed to load teleport data for " + regionName + ": " + e.getMessage());
            return false;
        }

        // Add player to region
        GameManager.addPlayerToRegion(player.getUniqueId(), player.getName(), regionName);
        sender.sendMessage(ChatColor.GREEN + "You joined region '" + regionName + "'.");
        BlockParty.getInstance().debugLog("Player " + player.getName() + " joined region: " + regionName);

        GameSession session = GameManager.getGameSession(regionName);
        if (session != null) {
            session.handlePlayerCountChanged();
        }

        // Auto-start once minimum players are present.
        int currentPlayers = GameManager.getPlayerCountInRegion(regionName);
        if (!GameManager.hasActiveSession(regionName) && currentPlayers >= minPlayers) {
            try {
                GameManager.startGameSession(regionName);
                BlockParty.getInstance().debugLog("Auto-started BlockParty in region " + regionName + " at " + currentPlayers + " players (min: " + minPlayers + ")");
            } catch (Exception e) {
                BlockParty.getInstance().errorLog("Failed to auto-start game in region " + regionName + ": " + e.getMessage());
            }
        }

        return true;
    }

    public static java.util.List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], RegionManager.getRegionNames(), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
