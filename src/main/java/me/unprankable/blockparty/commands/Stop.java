package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Stop {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty stop <region_name>");
            BlockParty.getInstance().debugLog("Stop command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Stop command: Region file not found for " + regionName);
            return false;
        }

        // Get current players in the region
        List<String> playersInRegion = GameManager.getPlayerNamesInRegion(regionName);
        int playerCount = playersInRegion.size();

        if (playerCount == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No active game in region '" + regionName + "'.");
            BlockParty.getInstance().debugLog("Stop command: No players in region " + regionName);
            return true;
        }

        // Stop the game session
        GameManager.stopGameSession(regionName);

        sender.sendMessage(ChatColor.GREEN + "BlockParty game stopped for region '" + regionName + "'.");
        sender.sendMessage(ChatColor.YELLOW + "Players removed: " + String.join(", ", playersInRegion));
        BlockParty.getInstance().debugLog("BlockParty game stopped for region: " + regionName + " (removed " + playerCount + " players) by " + sender.getName());
        return true;
    }
}
