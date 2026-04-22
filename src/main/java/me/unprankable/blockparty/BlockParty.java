package me.unprankable.blockparty;

import org.bukkit.plugin.java.JavaPlugin;
import me.unprankable.blockparty.managers.RegionManager;
import me.unprankable.blockparty.managers.GameManager;
import me.unprankable.blockparty.managers.StatsManager;
import me.unprankable.blockparty.managers.ConfigManager;
import me.unprankable.blockparty.listeners.PlayerSessionListener;

import java.util.logging.Logger;

public final class BlockParty extends JavaPlugin {
    private static BlockParty instance;
    private static RegionManager regionManager;
    @Override
    public void onEnable() {
        instance = this;
        ConfigManager.initialize();
        regionManager = new RegionManager(this.getDataFolder());
        StatsManager.initialize();
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(), this);
        commandExecutor executor = new commandExecutor(this);
        getCommand("blockparty").setExecutor(executor);
        getCommand("blockparty").setTabCompleter(executor);
        debugLog("BlockParty plugin enabled!");
        //load block party regions
    }

    public void debugLog(String message) {
        this.getLogger().info("[DEBUG] " + message);
    }
    public void errorLog(String message){
        this.getLogger().severe("[Error]" + message);
    }

    @Override
    public Logger getLogger() {
        return super.getLogger();
    }
    public static BlockParty getInstance(){
        return instance;
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        GameManager.stopAllGameSessions();
        GameManager.clearAllData();
        me.unprankable.blockparty.managers.GameSession.clearPendingHotbarRestores();
        StatsManager.close();
        debugLog("BlockParty plugin disabled!");
    }
}
