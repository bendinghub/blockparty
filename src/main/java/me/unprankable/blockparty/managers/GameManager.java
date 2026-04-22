package me.unprankable.blockparty.managers;

import java.util.*;

/**
 * Manages game state and player tracking for BlockParty regions
 */
public class GameManager {
    // Maps region name to list of player UUIDs
    private static final Map<String, List<UUID>> regionPlayers = new HashMap<>();
    // Maps player UUID to their current region
    private static final Map<UUID, String> playerRegions = new HashMap<>();
    // Maps player UUID to player name for logging
    private static final Map<UUID, String> playerNames = new HashMap<>();
    // Maps region name to active game session
    private static final Map<String, GameSession> activeSessions = new HashMap<>();

    /**
     * Add a player to a region
     */
    public static void addPlayerToRegion(UUID playerId, String playerName, String regionName) {
        playerRegions.put(playerId, regionName);
        playerNames.put(playerId, playerName);
        regionPlayers.computeIfAbsent(regionName, k -> new ArrayList<>()).add(playerId);
    }

    /**
     * Remove a player from a region
     */
    public static void removePlayerFromRegion(UUID playerId, String regionName) {
        playerRegions.remove(playerId);
        playerNames.remove(playerId);
        List<UUID> players = regionPlayers.get(regionName);
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                regionPlayers.remove(regionName);
            }
        }
    }

    /**
     * Get the region a player is currently in
     */
    public static String getPlayerRegion(UUID playerId) {
        return playerRegions.get(playerId);
    }

    /**
     * Get all players in a region
     */
    public static List<UUID> getPlayersInRegion(String regionName) {
        return regionPlayers.getOrDefault(regionName, new ArrayList<>());
    }

    /**
     * Get the number of players in a region
     */
    public static int getPlayerCountInRegion(String regionName) {
        List<UUID> players = regionPlayers.get(regionName);
        return players != null ? players.size() : 0;
    }

    /**
     * Get the player names in a region
     */
    public static List<String> getPlayerNamesInRegion(String regionName) {
        List<UUID> players = regionPlayers.get(regionName);
        List<String> names = new ArrayList<>();
        if (players != null) {
            for (UUID playerId : players) {
                String name = playerNames.get(playerId);
                if (name != null) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    /**
     * Get the tracked name for a player UUID
     */
    public static String getPlayerName(UUID playerId) {
        return playerNames.get(playerId);
    }

    /**
     * Clear all players from a region (e.g., when game ends)
     */
    public static void clearRegion(String regionName) {
        List<UUID> players = regionPlayers.get(regionName);
        if (players != null) {
            for (UUID playerId : new ArrayList<>(players)) {
                removePlayerFromRegion(playerId, regionName);
            }
        }
    }

    /**
     * Clear all game data (e.g., on plugin reload)
     */
    public static void clearAllData() {
        regionPlayers.clear();
        playerRegions.clear();
        playerNames.clear();
        activeSessions.clear();
    }

    /**
     * Start a game session for a region
     */
    public static void startGameSession(String regionName) throws Exception {
        startGameSession(regionName, false);
    }

    /**
     * Start a game session for a region, optionally skipping the pregame wait phase.
     */
    public static void startGameSession(String regionName, boolean skipWaiting) throws Exception {
        if (activeSessions.containsKey(regionName)) {
            throw new IllegalStateException("Game session already active for region: " + regionName);
        }
        GameSession session = new GameSession(regionName);
        session.start(skipWaiting);
        activeSessions.put(regionName, session);
    }

    /**
     * Stop a game session for a region
     */
    public static void stopGameSession(String regionName) {
        GameSession session = activeSessions.remove(regionName);
        if (session != null) {
            session.stop();
        }
    }

    /**
     * Stop every active game session and return how many were stopped.
     */
    public static int stopAllGameSessions() {
        List<String> regionNames = new ArrayList<>(activeSessions.keySet());
        int stopped = 0;
        for (String regionName : regionNames) {
            GameSession session = activeSessions.remove(regionName);
            if (session != null) {
                session.stop();
                stopped++;
            }
        }
        return stopped;
    }

    /**
     * Unregister an active session after it ends naturally
     */
    public static void finishGameSession(String regionName) {
        activeSessions.remove(regionName);
    }

    /**
     * Get the active game session for a region
     */
    public static GameSession getGameSession(String regionName) {
        return activeSessions.get(regionName);
    }

    /**
     * Check if a region has an active game session
     */
    public static boolean hasActiveSession(String regionName) {
        return activeSessions.containsKey(regionName);
    }
}

