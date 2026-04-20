package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.ConfigManager;
import me.unprankable.blockparty.managers.GameManager;
import me.unprankable.blockparty.managers.GameSession;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Join {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty join <region_name>");
            BlockParty.getInstance().debugLog("Join command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String regionName = args[1];
        Player player = (Player) sender;

        // Check if region exists
        Path regionsDir = Paths.get(BlockParty.getInstance().getDataFolder().getPath(), "regions");
        File regionFile = new File(regionsDir.toFile(), regionName + ".json");

        if (!regionFile.exists()) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' does not exist.");
            BlockParty.getInstance().debugLog("Join command: Region file not found for " + regionName);
            return false;
        }

        if (!ConfigManager.isMidGameJoinAllowed() && GameManager.hasActiveSession(regionName)) {
            sender.sendMessage(ChatColor.RED + "A game is already active in this region.");
            return false;
        }

        // Check if player is already in a region
        String currentRegion = GameManager.getPlayerRegion(player.getUniqueId());
        if (currentRegion != null) {
            sender.sendMessage(ChatColor.RED + "You are already in region '" + currentRegion + "'. Use /blockparty leave first.");
            BlockParty.getInstance().debugLog("Join command: Player " + player.getName() + " tried to join " + regionName + " while in " + currentRegion);
            return false;
        }

        // Add player to region
        GameManager.addPlayerToRegion(player.getUniqueId(), player.getName(), regionName);
        if (GameManager.hasActiveSession(regionName)) {
            GameSession session = GameManager.getGameSession(regionName);
            if (session != null) {
                session.registerParticipant(player.getUniqueId(), player.getName());
            }
        }
        sender.sendMessage(ChatColor.GREEN + "You joined region '" + regionName + "'.");
        BlockParty.getInstance().debugLog("Player " + player.getName() + " joined region: " + regionName);
        return true;
    }
}
