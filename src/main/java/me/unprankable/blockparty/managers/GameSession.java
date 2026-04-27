package me.unprankable.blockparty.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.events.GameEndEvent;
import me.unprankable.blockparty.events.PlayerEliminatedEvent;
import me.unprankable.blockparty.events.PlayerLeaveRegionEvent.RegionLeaveCause;
import me.unprankable.blockparty.events.RoundStartEvent;
import me.unprankable.blockparty.utils.MaterialUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GameSession {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Random RANDOM = new Random();
    private static final Map<UUID, ItemStack[]> pendingHotbarRestores = new HashMap<>();

    private final String regionName;
    private List<String> blockTypes;
    private int minPlayers = 2;
    private int pos1x, pos1y, pos1z;
    private int pos2x, pos2y, pos2z;
    private String worldName;
    private BukkitTask gameTask;
    private BukkitTask waitingTask;
    private BukkitTask prepTask;
    private BukkitTask eliminationTask;
    private int roundNumber = 0;
    private int waitingSecondsRemaining = ConfigManager.getWaitingForPlayersTime();
    private boolean roundsStarted = false;
    private boolean enforceMinPlayers = true;
    private boolean preservePattern = false;
    private boolean isActive = false;
    private Map<String, Integer> savedBlocks; // Save original block data for restoration
    private final Map<String, Integer> preservedPatternSlots = new HashMap<>();
    private int preservedPatternPaletteSize = 0;
    private final Map<UUID, ItemStack[]> originalHotbars = new HashMap<>();
    private final Set<UUID> matchParticipants = new LinkedHashSet<>();
    private final Map<UUID, String> matchParticipantNames = new HashMap<>();
    private final Map<UUID, GameMode> eliminatedPlayerGamemodes = new HashMap<>(); // Map of eliminated players to the game-mode they were in before elimination
    private final Set<String> eliminatedPlayers = new HashSet<>();

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

            Object minPlayersObj = regionData.get("minPlayers");
            if (minPlayersObj instanceof Number) {
                this.minPlayers = ((Number) minPlayersObj).intValue();
            }

            Object preservePatternObj = regionData.get("preservePattern");
            if (preservePatternObj instanceof Boolean) {
                this.preservePattern = (Boolean) preservePatternObj;
            }
        }
    }

    public void start() {
        start(false);
    }

    public void start(boolean skipWaiting) {
        if (isActive) {
            BlockParty.getInstance().errorLog("Game session for " + regionName + " is already active");
            return;
        }

        isActive = true;
        roundNumber = 0;
        roundsStarted = false;
        waitingSecondsRemaining = ConfigManager.getWaitingForPlayersTime();
        enforceMinPlayers = !skipWaiting;
        preservedPatternSlots.clear();
        preservedPatternPaletteSize = 0;
        BlockParty.getInstance().debugLog("Starting game session for region: " + regionName);

        if (skipWaiting) {
            beginPreparationPhase();
        } else {
            startWaitingPhase();
        }
    }

    public void savePlayerInfo() {
        // Snapshot all participants so stats include eliminated/offline players too.
        matchParticipants.clear();
        matchParticipantNames.clear();
        originalHotbars.clear();
        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            matchParticipants.add(playerId);
            String trackedName = GameManager.getPlayerName(playerId);
            Player onlinePlayer = Bukkit.getPlayer(playerId);
            if (onlinePlayer != null) {
                trackedName = onlinePlayer.getName();
                originalHotbars.put(playerId, snapshotHotbar(onlinePlayer));
            }
            if (trackedName != null) {
                matchParticipantNames.put(playerId, trackedName);
            }
        }

        // Save original blocks if restoration is enabled
        if (ConfigManager.isBlockRestorationEnabled()) {
            saveBlocks();
        }
    }

    public void handlePlayerCountChanged() {
        if (!isActive) {
            return;
        }

        int currentPlayers = GameManager.getPlayerCountInRegion(regionName);

        if (!roundsStarted) {
            if (enforceMinPlayers && currentPlayers < minPlayers) {
                resetPregameCountdown();
            } else if (enforceMinPlayers && waitingTask == null && prepTask == null) {
                startWaitingPhase();
            }
            return;
        }

        if (currentPlayers - eliminatedPlayers.size() <= 1) {
            endGame();
        }
    }

    public void resetPlayerGamemodeIfEliminated(UUID playerId) {
        if (eliminatedPlayerGamemodes.containsKey(playerId)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                double midX = (pos1x + pos2x) / 2.0 + 0.5;
                double midY = (pos1y + pos2y) / 2.0 + 1;
                double midZ = (pos1z + pos2z) / 2.0 + 0.5;
                player.teleport(new Location(player.getWorld(), midX, midY, midZ));
                player.setGameMode(eliminatedPlayerGamemodes.get(playerId));
            }
        }
    }

    /**
     * Unmarks a player as eliminated. Doesn't affect stats
     */
    public void uneliminate(UUID playerId) {
        eliminatedPlayerGamemodes.remove(playerId);
        String playerName = GameManager.getPlayerName(playerId);
        eliminatedPlayers.remove(playerName);
    }

    /**
     * true if rounds have started
     */
    public boolean haveRoundsStarted() {
        return roundsStarted;
    }

    public boolean isEliminated(UUID playerId) {
        return eliminatedPlayerGamemodes.containsKey(playerId);
    }

    private void startWaitingPhase() {
        cancelWaitingTask();
        cancelPrepTask();
        waitingSecondsRemaining = ConfigManager.getWaitingForPlayersTime();

        waitingTask = Bukkit.getScheduler().runTaskTimer(BlockParty.getInstance(), () -> {
            if (!isActive) {
                cancelWaitingTask();
                return;
            }

            int currentPlayers = GameManager.getPlayerCountInRegion(regionName);
            if (currentPlayers < minPlayers) {
                resetPregameCountdown();
                return;
            }

            sendWaitingActionBar(waitingSecondsRemaining);

            if (waitingSecondsRemaining <= 0) {
                cancelWaitingTask();
                beginPreparationPhase();
                return;
            }

            waitingSecondsRemaining--;
        }, 0L, 20L);
    }

    private void beginPreparationPhase() {
        if (!isActive) {
            return;
        }

        int currentPlayers = GameManager.getPlayerCountInRegion(regionName);
        if (enforceMinPlayers && currentPlayers < minPlayers) {
            resetPregameCountdown();
            return;
        }

        int prepTime = ConfigManager.getPreparationTime();
        broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameStartMessage().replace("%time%", String.valueOf(prepTime)));

        long prepDelayTicks = Math.max(0, prepTime) * 20L;
        if (prepDelayTicks == 0L) {
            startNextRound();
            return;
        }

        prepTask = Bukkit.getScheduler().runTaskLater(BlockParty.getInstance(), () -> {
            if (!isActive) {
                return;
            }
            if (GameManager.getPlayerCountInRegion(regionName) < minPlayers) {
                resetPregameCountdown();
                return;
            }
            startNextRound();
        }, prepDelayTicks);
    }

    private void resetPregameCountdown() {
        cancelWaitingTask();
        cancelPrepTask();
        waitingSecondsRemaining = ConfigManager.getWaitingForPlayersTime();
        sendWaitingPausedActionBar();
    }

    private void cancelWaitingTask() {
        if (waitingTask != null) {
            waitingTask.cancel();
            waitingTask = null;
        }
    }

    private void cancelPrepTask() {
        if (prepTask != null) {
            prepTask.cancel();
            prepTask = null;
        }
    }

    private void cancelEliminationTask() {
        if (eliminationTask != null) {
            eliminationTask.cancel();
            eliminationTask = null;
        }
    }

    private ItemStack[] snapshotHotbar(Player player) {
        ItemStack[] snapshot = new ItemStack[9];
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            snapshot[slot] = item == null ? null : item.clone();
        }
        return snapshot;
    }

    public void restoreHotbar(UUID playerId) {
        ItemStack[] snapshot = originalHotbars.get(playerId);
        if (snapshot == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            pendingHotbarRestores.put(playerId, snapshot);
            return;
        }

        applyHotbarSnapshot(player, snapshot);
    }

    private static void applyHotbarSnapshot(Player player, ItemStack[] snapshot) {
        for (int slot = 0; slot < snapshot.length; slot++) {
            player.getInventory().setItem(slot, snapshot[slot] == null ? null : snapshot[slot].clone());
        }
        player.updateInventory();
    }

    public static void restorePendingHotbar(Player player) {
        ItemStack[] snapshot = pendingHotbarRestores.remove(player.getUniqueId());
        if (snapshot != null) {
            applyHotbarSnapshot(player, snapshot);
        }
    }

    public static void clearPendingHotbarRestores() {
        pendingHotbarRestores.clear();
    }

    private void sendWaitingActionBar(int secondsRemaining) {
        String message = ChatColor.YELLOW + ConfigManager.getWaitingForPlayersMessage().replace("%time%", String.valueOf(secondsRemaining));
        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            }
        }
    }

    private void sendWaitingPausedActionBar() {
        String message = ChatColor.YELLOW + ConfigManager.getWaitingForMorePlayersMessage();
        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            }
        }
    }

    private void saveBlocks() {
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        savedBlocks = new HashMap<>();
        int minX = Math.min(pos1x, pos2x);
        int maxX = Math.max(pos1x, pos2x);
        int y = pos1y; // Floor is at a single Y level
        int minZ = Math.min(pos1z, pos2z);
        int maxZ = Math.max(pos1z, pos2z);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);
                String key = x + "," + y + "," + z;
                savedBlocks.put(key, block.getType().ordinal());
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

        if (roundNumber == 0) {
            savePlayerInfo();
        }

        roundsStarted = true;
        cancelWaitingTask();
        cancelPrepTask();

        roundNumber++;

        String selectedBlockName = blockTypes.get(RANDOM.nextInt(blockTypes.size()));
        Material selectedMaterialFromConfig = MaterialUtils.fromFriendlyName(selectedBlockName);

        if (selectedMaterialFromConfig == null) {
            BlockParty.getInstance().errorLog("Invalid material name in region file: " + selectedBlockName);
            broadcastToPlayers(ChatColor.RED + "Error: Invalid block in region config. Game cannot continue.");
            endGame();
            return;
        }

        RoundStartEvent event = new RoundStartEvent(roundNumber, regionName, selectedMaterialFromConfig, this);
        Bukkit.getPluginManager().callEvent(event);
        Material selectedMaterial = event.getChosenBlock();
        if (selectedMaterial != selectedMaterialFromConfig) {
            selectedBlockName = selectedMaterial.name();
        }

        // Calculate time remaining (decreases each round) - using config values
        int initialTime = ConfigManager.getInitialRoundTime();
        int minimumTime = ConfigManager.getMinimumRoundTime();
        int decreasePerRound = ConfigManager.getTimeDecreasePerRound();
        int timeRemaining = Math.max(minimumTime, initialTime - (roundNumber * decreasePerRound));

        broadcastToPlayers(ChatColor.YELLOW + ConfigManager.getRoundAnnouncementMessage().replace("%round%", String.valueOf(roundNumber)));
        broadcastToPlayers(ChatColor.AQUA + ConfigManager.getSelectedBlockMessage().replace("%block%", selectedBlockName));
        broadcastToPlayers(ChatColor.YELLOW + ConfigManager.getTimeWarningMessage()
                .replace("%block%", selectedBlockName)
                .replace("%time%", String.valueOf(timeRemaining)));
        broadcastToPlayers(ChatColor.GRAY + "(Time gets shorter each round)");

        fillPlayersHotbar(selectedMaterial);

        // Change the floor to a random assortment of blocks from the list
        setFloorBlocks();


        // Schedule the block removal
        gameTask = Bukkit.getScheduler().runTaskLater(BlockParty.getInstance(), () -> {
            removeNonSelectedBlocks(selectedMaterial);
            scheduleEliminationCheck();
        }, timeRemaining * 20L);
    }

    private void scheduleEliminationCheck() {
        cancelEliminationTask();
        eliminationTask = Bukkit.getScheduler().runTaskLater(BlockParty.getInstance(), () -> {
            if (isActive) {
                checkPlayersBelowFloor();
            }
        }, Math.max(0, ConfigManager.getEliminationCheckDelay()) * 20L);
    }

    private void setFloorBlocks() {
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        int minX = Math.min(pos1x, pos2x);
        int maxX = Math.max(pos1x, pos2x);
        int y = pos1y; // Floor is at a single Y level
        int minZ = Math.min(pos1z, pos2z);
        int maxZ = Math.max(pos1z, pos2z);

        List<Material> materials = new ArrayList<>();
        for (String blockName : blockTypes) {
            Material mat = MaterialUtils.fromFriendlyName(blockName);
            if (mat != null) {
                materials.add(mat);
            }
        }

        if (materials.isEmpty()) {
            BlockParty.getInstance().errorLog("No valid materials found for region " + regionName);
            return;
        }

        if (preservePattern) {
            setFloorBlocksWithPreservedPattern(world, minX, maxX, y, minZ, maxZ, materials);
            return;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Material randomMaterial = materials.get(RANDOM.nextInt(materials.size()));
                world.getBlockAt(x, y, z).setType(randomMaterial, false);
            }
        }
    }

    private void setFloorBlocksWithPreservedPattern(org.bukkit.World world, int minX, int maxX, int y, int minZ, int maxZ, List<Material> materials) {
        ensurePatternSlots(world, minX, maxX, y, minZ, maxZ, materials);

        // Preserve slot layout and only randomize which material each slot index maps to this round.
        List<Material> shuffledMaterials = new ArrayList<>(materials);
        Collections.shuffle(shuffledMaterials, RANDOM);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                String key = x + "," + z;
                int index = preservedPatternSlots.getOrDefault(key, 0);
                Material mappedMaterial = shuffledMaterials.get(Math.floorMod(index, shuffledMaterials.size()));
                world.getBlockAt(x, y, z).setType(mappedMaterial, false);
            }
        }
    }

    private void ensurePatternSlots(org.bukkit.World world, int minX, int maxX, int y, int minZ, int maxZ, List<Material> materials) {
        int paletteSize = materials.size();
        if (paletteSize <= 0) {
            return;
        }

        int expectedSize = (maxX - minX + 1) * (maxZ - minZ + 1);
        if (!preservedPatternSlots.isEmpty() && preservedPatternPaletteSize == paletteSize && preservedPatternSlots.size() == expectedSize) {
            return;
        }

        preservedPatternSlots.clear();
        preservedPatternPaletteSize = paletteSize;

        Map<Material, Integer> materialToIndex = new HashMap<>();
        for (int i = 0; i < materials.size(); i++) {
            materialToIndex.putIfAbsent(materials.get(i), i);
        }

        // Use the current floor at game start as the fixed slot layout.
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Material floorMaterial = world.getBlockAt(x, y, z).getType();
                Integer slotIndex = materialToIndex.get(floorMaterial);
                if (slotIndex == null) {
                    // Deterministic fallback keeps unknown blocks stable across rounds.
                    slotIndex = Math.floorMod((x * 73428767) ^ (z * 912931), paletteSize);
                }
                preservedPatternSlots.put(x + "," + z, slotIndex);
            }
        }
    }


    private void fillPlayersHotbar(Material selectedMaterial) {
        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            fillPlayerHotbar(player, selectedMaterial);
        }
    }

    private void fillPlayerHotbar(Player player, Material selectedMaterial) {
        ItemStack stack = new ItemStack(selectedMaterial, 64);
        for (int slot = 0; slot < 9; slot++) {
            player.getInventory().setItem(slot, stack.clone());
        }
    }


    private void removeNonSelectedBlocks(Material selectedMaterial) {
        if (worldName == null) {
            BlockParty.getInstance().errorLog("World name is null for region " + regionName);
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) {
            BlockParty.getInstance().errorLog("World not found: " + worldName);
            return;
        }

        int minX = Math.min(pos1x, pos2x);
        int maxX = Math.max(pos1x, pos2x);
        int y = pos1y; // Floor is at a single Y level
        int minZ = Math.min(pos1z, pos2z);
        int maxZ = Math.max(pos1z, pos2z);

        // Remove all blocks that are not the selected type
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() != selectedMaterial) {
                    block.setType(Material.AIR);
                }
            }
        }

        broadcastToPlayers(ChatColor.RED + ConfigManager.getBlocksRemovedMessage());
    }

    private void checkPlayersBelowFloor() {
        org.bukkit.World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        int floorY = Math.min(pos1y, pos2y);
        List<UUID> playersInRegion = new ArrayList<>(GameManager.getPlayersInRegion(regionName));
        List<String> eliminated = new ArrayList<>();

        for (UUID playerId : playersInRegion) {
            if (eliminatedPlayerGamemodes.containsKey(playerId)) {
                continue; // Already eliminated
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                GameManager.removePlayerFromRegion(playerId, regionName, RegionLeaveCause.DISCONNECT);
                eliminated.add("(disconnected)");
                continue;
            }

            if (player.getLocation().getY() < floorY) {
                // Player fell below the floor Y level - eliminate
                // Check PlayerEliminatedEvent first
                PlayerEliminatedEvent event = new PlayerEliminatedEvent(player, regionName, this);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    eliminated.add(player.getName());
                    // Record the elimination if enabled in config
                    if (ConfigManager.isTrackEliminationsEnabled()) {
                        StatsManager.recordElimination(player.getName());
                    }
                    // Eliminate the player
                    eliminatedPlayerGamemodes.put(playerId, player.getGameMode());
                    eliminatedPlayers.add(player.getName());
                    double midX = (pos1x + pos2x) / 2.0 + 0.5;
                    double midY = (pos1y + pos2y) / 2.0 + 1;
                    double midZ = (pos1z + pos2z) / 2.0 + 0.5;
                    player.teleport(new Location(player.getWorld(), midX, midY, midZ));
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }

        if (!eliminated.isEmpty()) {
            broadcastToPlayers(ChatColor.RED + "Eliminated: " + String.join(", ", eliminated));
        }

        // Check if game is over
        int remainingPlayers = GameManager.getPlayerCountInRegion(regionName) - eliminatedPlayers.size();
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
        isActive = false;
        roundsStarted = false;
        cancelWaitingTask();
        cancelPrepTask();
        cancelEliminationTask();
        String winner = "Nobody";
        boolean hasWinner = false;
        int remainingPlayers = GameManager.getPlayerCountInRegion(regionName) - eliminatedPlayers.size();

        if (remainingPlayers == 1) {
            List<String> winners = GameManager.getPlayerNamesInRegion(regionName);
            winners.removeAll(eliminatedPlayers);
            if (!winners.isEmpty()) {
                winner = winners.get(0);
                hasWinner = true;
            }
        }
        GameEndEvent event = new GameEndEvent(winner, hasWinner, regionName, this);
        Bukkit.getPluginManager().callEvent(event);
        if (event.hasWinner()) {
            broadcastToPlayers(ChatColor.GOLD + ConfigManager.getGameOverMessage());
            broadcastToPlayers(ChatColor.GREEN + ConfigManager.getWinnerMessage().replace("%player%", event.getWinner()));

            // Record the win if enabled in config
            if (ConfigManager.isStatsEnabled()) {
                StatsManager.recordGameWon(event.getWinner());
            }
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

        // Restore blocks to original state
        restoreBlocks();

        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            restoreHotbar(playerId);
            resetPlayerGamemodeIfEliminated(playerId);
        }
 
         // Fully tear down the session and remove all players from the region state.
         GameManager.finishGameSession(regionName);
         GameManager.clearRegion(regionName);
         preservedPatternSlots.clear();
         preservedPatternPaletteSize = 0;
         originalHotbars.clear();
 
         if (gameTask != null) {
             gameTask.cancel();
             gameTask = null;
         }
    }

    public void stop() {
        broadcastToPlayers(ChatColor.RED + "Game stopped by administrator.");
        endGame();
    }

    private void broadcastToPlayers(String message) {
        for (UUID playerId : GameManager.getPlayersInRegion(regionName)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}
