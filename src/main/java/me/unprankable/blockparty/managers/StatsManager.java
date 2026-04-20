package me.unprankable.blockparty.managers;

import me.unprankable.blockparty.BlockParty;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class StatsManager {
    private static Connection connection;
    private static final String DATABASE_NAME = "blockparty_stats.db";

    public static void initialize() {
        try {
            String dbPath = BlockParty.getInstance().getDataFolder() + "/" + DATABASE_NAME;
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
            BlockParty.getInstance().debugLog("StatsManager initialized with database at: " + dbPath);
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to initialize StatsManager: " + e.getMessage());
        }
    }

    private static void createTables() {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                "CREATE TABLE IF NOT EXISTS player_stats (" +
                "  player_name TEXT PRIMARY KEY," +
                "  games_played INT DEFAULT 0," +
                "  games_won INT DEFAULT 0," +
                "  best_round INT DEFAULT 0," +
                "  total_eliminations INT DEFAULT 0," +
                "  last_played LONG DEFAULT 0" +
                ")"
            );
            BlockParty.getInstance().debugLog("Player stats table created/verified");
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to create stats table: " + e.getMessage());
        }
    }

    /**
     * Record a game played by a player
     */
    public static void recordGamePlayed(String playerName, int roundReached) {
        try (Statement statement = connection.createStatement()) {
            String escapedName = playerName.replace("'", "''");
            statement.execute(
                "INSERT OR IGNORE INTO player_stats (player_name) VALUES ('" + escapedName + "')"
            );
            statement.execute(
                "UPDATE player_stats SET " +
                "  games_played = games_played + 1," +
                "  best_round = MAX(best_round, " + roundReached + ")," +
                "  last_played = " + System.currentTimeMillis() +
                " WHERE player_name = '" + escapedName + "'"
            );
            BlockParty.getInstance().debugLog("Recorded game played for " + playerName + " (round: " + roundReached + ")");
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to record game played: " + e.getMessage());
        }
    }

    /**
     * Record a game won by a player
     */
    public static void recordGameWon(String playerName) {
        try (Statement statement = connection.createStatement()) {
            String escapedName = playerName.replace("'", "''");
            statement.execute(
                "INSERT OR IGNORE INTO player_stats (player_name) VALUES ('" + escapedName + "')"
            );
            statement.execute(
                "UPDATE player_stats SET " +
                "  games_won = games_won + 1," +
                "  last_played = " + System.currentTimeMillis() +
                " WHERE player_name = '" + escapedName + "'"
            );
            BlockParty.getInstance().debugLog("Recorded game won for " + playerName);
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to record game won: " + e.getMessage());
        }
    }

    /**
     * Record eliminations by a player
     */
    public static void recordElimination(String playerName) {
        try (Statement statement = connection.createStatement()) {
            String escapedName = playerName.replace("'", "''");
            statement.execute(
                "INSERT OR IGNORE INTO player_stats (player_name) VALUES ('" + escapedName + "')"
            );
            statement.execute(
                "UPDATE player_stats SET " +
                "  total_eliminations = total_eliminations + 1 " +
                "WHERE player_name = '" + escapedName + "'"
            );
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to record elimination: " + e.getMessage());
        }
    }

    /**
     * Get player stats
     */
    public static Map<String, Object> getPlayerStats(String playerName) {
        Map<String, Object> stats = new HashMap<>();
        try (Statement statement = connection.createStatement()) {
            String escapedName = playerName.replace("'", "''");
            ResultSet result = statement.executeQuery(
                "SELECT * FROM player_stats WHERE player_name = '" + escapedName + "'"
            );
            
            if (result.next()) {
                stats.put("player_name", result.getString("player_name"));
                stats.put("games_played", result.getInt("games_played"));
                stats.put("games_won", result.getInt("games_won"));
                stats.put("best_round", result.getInt("best_round"));
                stats.put("total_eliminations", result.getInt("total_eliminations"));
                stats.put("last_played", result.getLong("last_played"));
                
                int gamesPlayed = result.getInt("games_played");
                int gamesWon = result.getInt("games_won");
                double winRate = gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
                stats.put("win_rate", String.format("%.2f", winRate));
            } else {
                stats.put("not_found", true);
            }
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to get player stats: " + e.getMessage());
            stats.put("error", true);
        }
        return stats;
    }

    /**
     * Get top players by wins
     */
    public static Map<String, Object> getTopPlayers(int limit) {
        Map<String, Object> topPlayers = new HashMap<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet result = statement.executeQuery(
                "SELECT player_name, games_won, games_played FROM player_stats " +
                "ORDER BY games_won DESC LIMIT " + limit
            );
            
            int rank = 1;
            while (result.next()) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("rank", rank);
                playerData.put("games_won", result.getInt("games_won"));
                playerData.put("games_played", result.getInt("games_played"));
                topPlayers.put(result.getString("player_name"), playerData);
                rank++;
            }
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to get top players: " + e.getMessage());
        }
        return topPlayers;
    }

    /**
     * Close the database connection
     */
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                BlockParty.getInstance().debugLog("Database connection closed");
            }
        } catch (SQLException e) {
            BlockParty.getInstance().errorLog("Failed to close database: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

