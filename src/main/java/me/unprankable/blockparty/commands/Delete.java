package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Delete {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty delete <region_name>");
            BlockParty.getInstance().debugLog("Delete command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Delete command: Region file not found for " + regionName);
            return false;
        }

        if (regionFile.delete()) {
            sender.sendMessage(ChatColor.GREEN + "Region '" + regionName + "' deleted successfully.");
            BlockParty.getInstance().debugLog("Region deleted: " + regionName + " by " + sender.getName());
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to delete region '" + regionName + "'.");
            BlockParty.getInstance().errorLog("Failed to delete region file: " + regionFile.getAbsolutePath());
            return false;
        }
    }
}
