package me.unprankable.blockparty.managers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.regions.Region;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.hooks.WorldEditHook;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RegionManager {
    private static File dataFolder;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public RegionManager(File pluginDataFolder){
        dataFolder = pluginDataFolder;
    }
    public static boolean createRegion(String name, Region region){
        return createRegion(name, region, new ArrayList<>(), 2, 0);
    }

    public static boolean createRegion(String name, Region region, List<String> blocks){
        return createRegion(name, region, blocks, 2, 0);
    }

    public static boolean createRegion(String name, Region region, List<String> blocks, int minPlayers){
        return createRegion(name, region, blocks, minPlayers, 0);
    }

    public static boolean createRegion(String name, Region region, List<String> blocks, int minPlayers, int numRounds){
        Map<String, Object> regionData = new LinkedHashMap<>();//looking at docs rn
        regionData.put("name",name);
        regionData.put("world", WorldEditHook.getWorldName(region));
        regionData.put("pos1", WorldEditHook.getFirstPosition(region));
        regionData.put("pos2", WorldEditHook.getSecondPosition(region));
        regionData.put("blocks", blocks.isEmpty() ? new ArrayList<>() : blocks);
        regionData.put("minPlayers", minPlayers);
        if (numRounds > 0) {
            regionData.put("numRounds", numRounds);
        }

        Path regionsDir = Paths.get(dataFolder.getPath(), "regions");
        try {
            Files.createDirectories(regionsDir);
        } catch (IOException e){
            BlockParty.getInstance().errorLog("Failed to create regions directory" + e.getMessage());
            return false;
        }
        File RegionFile = new File(regionsDir.toFile(), name + ".json");
        try (FileWriter writer = new FileWriter(RegionFile)){
            GSON.toJson(regionData, writer);
        } catch (IOException e){
            BlockParty.getInstance().errorLog("Failed to create regions file for" + name + " " + e.getMessage());
            return false;
        }
        return true;
    }
}
