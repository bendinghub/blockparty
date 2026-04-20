package me.unprankable.blockparty;

import org.bukkit.plugin.java.JavaPlugin;
import me.unprankable.blockparty.managers.RegionManager;
import me.unprankable.blockparty.managers.StatsManager;
import me.unprankable.blockparty.managers.ConfigManager;

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
        getCommand("blockparty").setExecutor(new commandExecutor(this));
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
        StatsManager.close();
        debugLog("BlockParty plugin disabled!");
    }
}
