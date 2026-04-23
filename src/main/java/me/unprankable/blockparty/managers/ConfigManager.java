package me.unprankable.blockparty.managers;

import me.unprankable.blockparty.BlockParty;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class ConfigManager {
    private static FileConfiguration config;
    private static final String CONFIG_FILE = "config.yml";

    public static void initialize() {
        File configFile = new File(BlockParty.getInstance().getDataFolder(), CONFIG_FILE);
        
        // Create config from defaults if it doesn't exist
        if (!configFile.exists()) {
            BlockParty.getInstance().getDataFolder().mkdirs();
            try (InputStream in = BlockParty.getInstance().getResource(CONFIG_FILE)) {
                Files.copy(in, configFile.toPath());
                BlockParty.getInstance().debugLog("Created default config.yml");
            } catch (IOException e) {
                BlockParty.getInstance().errorLog("Failed to create config.yml: " + e.getMessage());
            }
        }
        
        // Load the config
        config = YamlConfiguration.loadConfiguration(configFile);
        BlockParty.getInstance().debugLog("ConfigManager initialized");
    }

    // Game Settings
    public static int getInitialRoundTime() {
        return config.getInt("game.initial_round_time", 10);
    }

    public static int getMinimumRoundTime() {
        return config.getInt("game.minimum_round_time", 1);
    }

    public static int getTimeDecreasePerRound() {
        return config.getInt("game.time_decrease_per_round", 1);
    }

    public static int getDelayBetweenRounds() {
        return config.getInt("game.delay_between_rounds", 40);
    }

    public static int getPreparationTime() {
        return config.getInt("game.preparation_time", 5);
    }

    public static int getWaitingForPlayersTime() {
        return config.getInt("game.waiting_for_players_time", 30);
    }

    public static int getEliminationCheckDelay() {
        return config.getInt("game.elimination_check_delay", 10);
    }

    // Region Settings
    public static int getDefaultMinPlayers() {
        return config.getInt("regions.default_min_players", 2);
    }

    // Database Settings
    public static String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public static String getDatabaseFilename() {
        return config.getString("database.filename", "blockparty_stats.db");
    }

    public static String getDatabaseURL() {
        return config.getString("database.url", "jdbc:mysql://[host]:[port]/database");
    }

    public static String getDatabaseUsername() {
        return config.getString("database.username", "user");
    }

    public static String getDatabasePassword() {
        return config.getString("database.password", "blockparty123");
    }

    // Stats Settings
    public static boolean isStatsEnabled() {
        return config.getBoolean("stats.enabled", true);
    }

    public static boolean isTrackEliminationsEnabled() {
        return config.getBoolean("stats.track_eliminations", true);
    }

    public static boolean isEliminateOnLeave() {
        return config.getBoolean("stats.eliminate_on_leave", false);
    }

    public static boolean isTrackBestRoundsEnabled() {
        return config.getBoolean("stats.track_best_rounds", true);
    }

    // Messages
    public static String getGameStartMessage() {
        return config.getString("messages.game_start", "BlockParty game starting in %time% seconds!");
    }

    public static String getWaitingForPlayersMessage() {
        return config.getString("messages.waiting_for_players", "Waiting %time% seconds for more players...");
    }

    public static String getWaitingForMorePlayersMessage() {
        return config.getString("messages.waiting_for_more_players", "Waiting for more players to join...");
    }

    public static String getRoundAnnouncementMessage() {
        return config.getString("messages.round_announcement", "--- Round %round% ---");
    }

    public static String getSelectedBlockMessage() {
        return config.getString("messages.selected_block", "Selected block: %block%");
    }

    public static String getTimeWarningMessage() {
        return config.getString("messages.time_warning", "Get to a %block% block! You have %time% seconds!");
    }

    public static String getBlocksRemovedMessage() {
        return config.getString("messages.blocks_removed", "Blocks removed!");
    }

    public static String getPlayerEliminatedMessage() {
        return config.getString("messages.player_eliminated", "%player% was eliminated!");
    }

    public static String getGameOverMessage() {
        return config.getString("messages.game_over", "=== GAME OVER ===");
    }

    public static String getWinnerMessage() {
        return config.getString("messages.winner", "🏆 Last Player Standing: %player%");
    }

    // Logging Settings
    public static boolean isDebugLoggingEnabled() {
        return config.getBoolean("logging.debug", true);
    }

    public static boolean isPlayerActionLoggingEnabled() {
        return config.getBoolean("logging.log_player_actions", true);
    }

    public static boolean isGameStatsLoggingEnabled() {
        return config.getBoolean("logging.log_game_stats", true);
    }

    // Features
    public static boolean isMidGameJoinAllowed() {
        return config.getBoolean("features.allow_mid_game_join", false);
    }

    public static boolean isBlockRestorationEnabled() {
        return config.getBoolean("features.restore_blocks_after_game", true);
    }

    public static boolean isTeleportOnJoinEnabled() {
        return config.getBoolean("features.teleport_on_join", false);
    }

    public static void reload() {
        initialize();
        BlockParty.getInstance().debugLog("Config reloaded");
    }
}

