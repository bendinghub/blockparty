package me.unprankable.blockparty.managers;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sk89q.worldedit.regions.Region;
import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.hooks.WorldEditHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionManager {
    private static File dataFolder;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public RegionManager(File pluginDataFolder){
        dataFolder = pluginDataFolder;
    }

    public static boolean createRegion(String name, Region region){
        return createRegion(name, region, 2, false);
    }

    public static boolean createRegion(String name, Region region, int minPlayers){
        return createRegion(name, region, minPlayers, false);
    }

    public static boolean createRegion(String name, Region region, int minPlayers, boolean preservePattern){
        List<String> blocks = collectRegionBlocks(region);
        if (blocks.isEmpty()) {
            BlockParty.getInstance().errorLog("Failed to collect blocks for region '" + name + "'. Region may contain only air or an invalid world.");
            return false;
        }
        return createRegion(name, region, blocks, minPlayers, preservePattern);
    }


    public static boolean createRegion(String name, Region region, List<String> blocks){
        return createRegion(name, region, blocks, 2, false);
    }

    public static boolean createRegion(String name, Region region, List<String> blocks, int minPlayers){
        return createRegion(name, region, blocks, minPlayers, false);
    }

    public static boolean createRegion(String name, Region region, List<String> blocks, int minPlayers, boolean preservePattern){
        Map<String, Object> regionData = new LinkedHashMap<>();
        regionData.put("name", name);
        regionData.put("world", WorldEditHook.getWorldName(region));
        regionData.put("pos1", WorldEditHook.getFirstPosition(region));
        regionData.put("pos2", WorldEditHook.getSecondPosition(region));
        regionData.put("blocks", blocks.isEmpty() ? new ArrayList<>() : blocks);
        regionData.put("minPlayers", minPlayers);
        regionData.put("preservePattern", preservePattern);
        return writeRegionData(name, regionData);
    }

    public static boolean updateRegionOption(String regionName, String option, String value) {
        Map<String, Object> regionData = readRegionData(regionName);
        if (regionData == null) {
            return false;
        }

        String normalized = option.toLowerCase();
        switch (normalized) {
            case "name" -> {
                String newName = value.trim();
                if (newName.isEmpty()) {
                    return false;
                }
                regionData.put("name", newName);
                if (!writeRegionData(newName, regionData)) {
                    return false;
                }
                if (!newName.equals(regionName)) {
                    File oldFile = getRegionFile(regionName);
                    if (oldFile.exists() && !oldFile.delete()) {
                        BlockParty.getInstance().errorLog("Failed to delete old region file for rename: " + oldFile.getAbsolutePath());
                    }
                }
                return true;
            }
            case "minplayers" -> {
                try {
                    regionData.put("minPlayers", Integer.parseInt(value.trim()));
                } catch (NumberFormatException e) {
                    return false;
                }
                return writeRegionData(regionName, regionData);
            }
            case "blocks" -> {
                List<String> blocks = new ArrayList<>();
                for (String block : value.split(",")) {
                    String trimmed = block.trim();
                    if (!trimmed.isEmpty()) {
                        blocks.add(trimmed);
                    }
                }
                regionData.put("blocks", blocks);
                return writeRegionData(regionName, regionData);
            }
            case "preservepattern" -> {
                String normalizedValue = value.trim().toLowerCase();
                Boolean parsedValue = switch (normalizedValue) {
                    case "true", "yes", "on", "1" -> true;
                    case "false", "no", "off", "0" -> false;
                    default -> null;
                };
                if (parsedValue == null) {
                    return false;
                }
                regionData.put("preservePattern", parsedValue);
                return writeRegionData(regionName, regionData);
            }
            default -> {
                return false;
            }
        }
    }

    private static Map<String, Object> readRegionData(String regionName) {
        File regionFile = getRegionFile(regionName);
        if (!regionFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(regionFile)) {
            Map<String, Object> regionData = GSON.fromJson(reader, Map.class);
            return regionData == null ? null : new LinkedHashMap<>(regionData);
        } catch (IOException e) {
            BlockParty.getInstance().errorLog("Failed to read region file for " + regionName + ": " + e.getMessage());
            return null;
        }
    }

    private static boolean writeRegionData(String regionName, Map<String, Object> regionData) {
        Path regionsDir = Paths.get(dataFolder.getPath(), "regions");
        try {
            Files.createDirectories(regionsDir);
        } catch (IOException e) {
            BlockParty.getInstance().errorLog("Failed to create regions directory" + e.getMessage());
            return false;
        }

        File regionFile = getRegionFile(regionName);
        try (FileWriter writer = new FileWriter(regionFile)) {
            GSON.toJson(regionData, writer);
        } catch (IOException e) {
            BlockParty.getInstance().errorLog("Failed to create regions file for " + regionName + " " + e.getMessage());
            return false;
        }
        return true;
    }

    private static File getRegionFile(String regionName) {
        Path regionsDir = Paths.get(dataFolder.getPath(), "regions");
        return new File(regionsDir.toFile(), regionName + ".json");
    }

    private static List<String> collectRegionBlocks(Region region) {
        if (region == null) {
            return Collections.emptyList();
        }

        String worldName = WorldEditHook.getWorldName(region);
        if (worldName == null) {
            return Collections.emptyList();
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return Collections.emptyList();
        }

        int[] pos1 = WorldEditHook.getFirstPosition(region);
        int[] pos2 = WorldEditHook.getSecondPosition(region);
        if (pos1 == null || pos2 == null) {
            return Collections.emptyList();
        }

        int minX = Math.min(pos1[0], pos2[0]);
        int maxX = Math.max(pos1[0], pos2[0]);
        int minY = Math.min(pos1[1], pos2[1]);
        int maxY = Math.max(pos1[1], pos2[1]);
        int minZ = Math.min(pos1[2], pos2[2]);
        int maxZ = Math.max(pos1[2], pos2[2]);

        Set<String> uniqueBlocks = new LinkedHashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (material != Material.AIR) {
                        uniqueBlocks.add(material.name());
                    }
                }
            }
        }

        return new ArrayList<>(uniqueBlocks);
    }

    public static List<String> getRegionNames() {
        if (dataFolder == null) {
            return Collections.emptyList();
        }

        Path regionsDir = Paths.get(dataFolder.getPath(), "regions");
        File regionsDirFile = regionsDir.toFile();
        if (!regionsDirFile.exists()) {
            return Collections.emptyList();
        }

        File[] regionFiles = regionsDirFile.listFiles((dir, name) -> name.endsWith(".json"));
        if (regionFiles == null || regionFiles.length == 0) {
            return Collections.emptyList();
        }

        List<String> regionNames = new ArrayList<>();
        for (File file : regionFiles) {
            String fileName = file.getName();
            regionNames.add(fileName.substring(0, fileName.length() - 5));
        }
        Collections.sort(regionNames);
        return regionNames;
    }
}
