package me.unprankable.blockparty.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.unprankable.blockparty.BlockParty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class GameSession {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Random RANDOM = new Random();

    private final String regionName;
    private List<String> blockTypes;
    private int pos1x, pos1y, pos1z;
    private int pos2x, pos2y, pos2z;
    private String worldName;
    private BukkitTask gameTask;
    private int roundNumber = 0;
    private int maxRounds = 0;
    private boolean isActive = false;
    private Map<String, Integer> savedBlocks; // Save original block data for restoration
    private final Set<UUID> matchParticipants = new LinkedHashSet<>();
    private final Map<UUID, String> matchParticipantNames = new HashMap<>();

    public GameSession(String regionName) throws IOException {
        this.regionName = regionName;
        loadRegionData();
    }

    private void loadRegionData() throws IOException {
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        try (FileReader reader = new FileReader(regionFile)) {
            Map<String, Object> regionData = GSON.fromJson(reader, Map.class);

            if (regionData == null) {
                throw new IOException("Failed to parse region file");
            }

            this.worldName = (String) regionData.get("world");

            // Parse positions
            List<?> pos1List = (List<?>) regionData.get("pos1");
            this.pos1x = ((Number) pos1List.get(0)).intValue();
            this.pos1y = ((Number) pos1List.get(1)).intValue();
            this.pos1z = ((Number) pos1List.get(2)).intValue();

            List<?> pos2List = (List<?>) regionData.get("pos2");
            this.pos2x = ((Number) pos2List.get(0)).intValue();
            this.pos2y = ((Number) pos2List.get(1)).intValue();
            this.pos2z = ((Number) pos2List.get(2)).intValue();

            // Parse blocks
            this.blockTypes = new ArrayList<>();
            List<?> blocksList = (List<?>) regionData.get("blocks");
            if (blocksList != null) {
                for (Object block : blocksList) {
                    blockTypes.add((String) block);
                }
            }

            // Parse numRounds (default to 0 = unlimited)
            Object numRoundsObj = regionData.get("numRounds");
            this.maxRounds = numRoundsObj != null ? ((Number) numRoundsObj).intValue() : 0;
        }
    }

    public void start() {
        if (isActive) {
            BlockParty.getInstance().errorLog("Game session for " + regionName + " is already active");
            return;
        }

        isActive = true;
        roundNumber = 0;
        int prepTime = ConfigManager.getPreparationTime();
        BlockParty.getInstance().debugLog("Starting game session for region: " + regionName);

        // Snapshot all participants so stats include eliminated/offline players too.
        matchParticipants.clear();
        matchParticipantNames.clear();
        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            matchParticipants.add(playerId);
            String trackedName = GameManager.getPlayerName(playerId);
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer != null) {
                trackedName = onlinePlayer.getName();
            }
            if (trackedName != null) {
                matchParticipantNames.put(playerId, trackedName);
            }
        }
        
        // Save original blocks if restoration is enabled
        if (ConfigManager.isBlockRestorationEnabled()) {
            saveBlocks();
        }
        
        broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameStartMessage().replace("%time%", String.valueOf(prepTime)));

        long prepDelayTicks = Math.max(0, prepTime) * 20L;
        if (prepDelayTicks == 0L) {
            startNextRound();
        } else {
            gameTask = Bukkit.getScheduler().runTaskLater(BlockParty.getInstance(), this::startNextRound, prepDelayTicks);
        }
    }

    private void saveBlocks() {
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        savedBlocks = new HashMap<>();
        int minX = Math.min(pos1x, pos2x);
        int maxX = Math.max(pos1x, pos2x);
        int minY = Math.min(pos1y, pos2y);
        int maxY = Math.max(pos1y, pos2y);
        int minZ = Math.min(pos1z, pos2z);
        int maxZ = Math.max(pos1z, pos2z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    String key = x + "," + y + "," + z;
                    savedBlocks.put(key, block.getType().ordinal());
                }
            }
        }
        BlockParty.getInstance().debugLog("Saved " + savedBlocks.size() + " blocks for region: " + regionName);
    }

    private void restoreBlocks() {
        if (!ConfigManager.isBlockRestorationEnabled() || savedBlocks == null) {
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        for (String key : savedBlocks.keySet()) {
            String[] coords = key.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            int z = Integer.parseInt(coords[2]);
            
            Material material = Material.values()[savedBlocks.get(key)];
            world.getBlockAt(x, y, z).setType(material, false);
        }
        BlockParty.getInstance().debugLog("Restored " + savedBlocks.size() + " blocks for region: " + regionName);
    }

    private void startNextRound() {
        if (!isActive) {
            return;
        }

        // Check if max rounds reached
        if (maxRounds > 0 && roundNumber >= maxRounds) {
            broadcastToPlayers(ChatColor.YELLOW + "Maximum rounds reached!");
            List<String> survivors = GameManager.getPlayerNamesInRegion(regionName);
            if (survivors.size() == 1) {
                endGame(survivors.get(0), false);
            } else {
                if (!survivors.isEmpty()) {
                    broadcastToPlayers(ChatColor.YELLOW + "Round limit tie between: " + String.join(", ", survivors));
                }
                endGame(null, true);
            }
            return;
        }

        roundNumber++;
        String selectedBlock = blockTypes.get(RANDOM.nextInt(blockTypes.size()));

        // Calculate time remaining (decreases each round) - using config values
        int initialTime = ConfigManager.getInitialRoundTime();
        int minimumTime = ConfigManager.getMinimumRoundTime();
        int decreasePerRound = ConfigManager.getTimeDecreasePerRound();
        int timeRemaining = Math.max(minimumTime, initialTime - (roundNumber * decreasePerRound));

        broadcastToPlayers(ChatColor.YELLOW + ConfigManager.getRoundAnnouncementMessage().replace("%round%", String.valueOf(roundNumber)));
        broadcastToPlayers(ChatColor.AQUA + ConfigManager.getSelectedBlockMessage().replace("%block%", selectedBlock));
        broadcastToPlayers(ChatColor.YELLOW + ConfigManager.getTimeWarningMessage()
                .replace("%block%", selectedBlock)
                .replace("%time%", String.valueOf(timeRemaining)));
        broadcastToPlayers(ChatColor.GRAY + "(Time gets shorter each round)");

        // Schedule the block removal
        gameTask = Bukkit.getScheduler().runTaskLater(BlockParty.getInstance(), () -> {
            removeNonSelectedBlocks(selectedBlock);
            checkPlayersOnBlock(selectedBlock);
        }, timeRemaining * 20L);
    }

    private void removeNonSelectedBlocks(String selectedBlockType) {
        if (worldName == null) {
            BlockParty.getInstance().errorLog("World name is null for region " + regionName);
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            BlockParty.getInstance().errorLog("World not found: " + worldName);
            return;
        }

        Material selectedMaterial;
        try {
            selectedMaterial = Material.valueOf(selectedBlockType);
        } catch (IllegalArgumentException e) {
            BlockParty.getInstance().errorLog("Invalid material: " + selectedBlockType);
            return;
        }

        int minX = Math.min(pos1x, pos2x);
        int maxX = Math.max(pos1x, pos2x);
        int minY = Math.min(pos1y, pos2y);
        int maxY = Math.max(pos1y, pos2y);
        int minZ = Math.min(pos1z, pos2z);
        int maxZ = Math.max(pos1z, pos2z);

        // Remove all blocks that are not the selected type
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() != selectedMaterial && block.getType() != Material.AIR) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        broadcastToPlayers(ChatColor.RED + ConfigManager.getBlocksRemovedMessage());
    }

    private void checkPlayersOnBlock(String selectedBlockType) {
        Material selectedMaterial;
        try {
            selectedMaterial = Material.valueOf(selectedBlockType);
        } catch (IllegalArgumentException e) {
            BlockParty.getInstance().errorLog("Invalid material: " + selectedBlockType);
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        List<UUID> playersInRegion = new ArrayList<>(GameManager.getPlayersInRegion(regionName));
        List<String> eliminated = new ArrayList<>();

        for (UUID playerId : playersInRegion) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                GameManager.removePlayerFromRegion(playerId, regionName);
                eliminated.add("(disconnected)");
                continue;
            }

            Block blockBelow = player.getLocation().subtract(0, 1, 0).getBlock();
            if (blockBelow.getType() != selectedMaterial) {
                // Player is not on the selected block - eliminate
                eliminated.add(player.getName());
                // Record the elimination if enabled in config
                if (ConfigManager.isTrackEliminationsEnabled()) {
                    StatsManager.recordElimination(player.getName());
                }
                GameManager.removePlayerFromRegion(playerId, regionName);
                player.setHealth(0); // Eliminate the player
            }
        }

        if (!eliminated.isEmpty()) {
            broadcastToPlayers(ChatColor.RED + "Eliminated: " + String.join(", ", eliminated));
        }

        // Check if game is over
        int remainingPlayers = GameManager.getPlayerCountInRegion(regionName);
        if (remainingPlayers == 1) {
            endGame();
        } else if (remainingPlayers > 0) {
            // Schedule next round after a delay (from config)
            int delayBetweenRounds = ConfigManager.getDelayBetweenRounds();
            Bukkit.getScheduler().runTaskLater(BlockParty.getInstance(), this::startNextRound, delayBetweenRounds);
        } else {
            // No players left
            endGame();
        }
    }

    private void endGame() {
        endGame(null, false);
    }

    private void endGame(String forcedWinner, boolean tiedByRoundLimit) {
        isActive = false;
        String winner = "Nobody";
        int remainingPlayers = GameManager.getPlayerCountInRegion(regionName);

        if (forcedWinner != null) {
            winner = forcedWinner;
            broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameOverMessage());
            broadcastToPlayers(ChatColor.GREEN + ConfigManager.getWinnerMessage().replace("%player%", winner));
            if (ConfigManager.isStatsEnabled()) {
                StatsManager.recordGameWon(winner);
            }
        } else if (remainingPlayers == 1) {
            List<String> winners = GameManager.getPlayerNamesInRegion(regionName);
            if (!winners.isEmpty()) {
                winner = winners.get(0);
            }
            broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameOverMessage());
            broadcastToPlayers(ChatColor.GREEN + ConfigManager.getWinnerMessage().replace("%player%", winner));
            
            // Record the win if enabled in config
            if (ConfigManager.isStatsEnabled()) {
                StatsManager.recordGameWon(winner);
            }
        } else if (tiedByRoundLimit) {
            broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameOverMessage());
            broadcastToPlayers(ChatColor.YELLOW + "Round limit reached with multiple survivors. No winner this game.");
        } else {
            broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameOverMessage());
            broadcastToPlayers(ChatColor.YELLOW + "All players eliminated!");
        }

        // Record stats for all players who participated if enabled
        if (ConfigManager.isStatsEnabled()) {
            for (UUID playerId : matchParticipants) {
                String playerName = matchParticipantNames.get(playerId);
                if (playerName == null) {
                    Player onlinePlayer = Bukkit.getPlayer(playerId);
                    if (onlinePlayer != null) {
                        playerName = onlinePlayer.getName();
                    }
                }
                if (playerName != null) {
                    StatsManager.recordGamePlayed(playerName, roundNumber);
                }
            }
        }

        if (gameTask != null) {
            gameTask.cancel();
        }

        // Restore blocks if enabled
        restoreBlocks();

        GameManager.finishGameSession(regionName);
        GameManager.clearRegion(regionName);
        BlockParty.getInstance().debugLog("Game session ended for region: " + regionName + ", Winner: " + winner);
    }

    private void broadcastToPlayers(String message) {
        List<String> playerNames = GameManager.getPlayerNamesInRegion(regionName);
        for (String playerName : playerNames) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendMessage(message);
            }
        }
    }

    public void stop() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        broadcastToPlayers(ChatColor.RED + "Game stopped by administrator.");
        endGame();
    }

    public String getRegionName() {
        return regionName;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void registerParticipant(UUID playerId, String playerName) {
        if (playerId == null) {
            return;
        }
        matchParticipants.add(playerId);
        if (playerName != null && !playerName.isEmpty()) {
            matchParticipantNames.put(playerId, playerName);
        }
    }
}

