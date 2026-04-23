package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.events.PlayerLeaveRegionEvent.RegionLeaveCause;
import me.unprankable.blockparty.managers.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class Leave {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return false;
        }

        Player player = (Player) sender;

        // Check if player is in any region
        String currentRegion = GameManager.getPlayerRegion(player.getUniqueId());
        if (currentRegion == null) {
            sender.sendMessage(ChatColor.RED + "You are not in any BlockParty region.");
            BlockParty.getInstance().debugLog("Leave command: Player " + player.getName() + " tried to leave but is not in a region");
            return false;
        }

        // Remove player from region
        if (GameManager.removePlayerFromRegion(player.getUniqueId(), currentRegion, RegionLeaveCause.COMMAND)) {
            sender.sendMessage(ChatColor.GREEN + "You left region '" + currentRegion + "'.");
            BlockParty.getInstance().debugLog("Player " + player.getName() + " left region: " + currentRegion);
        }
        return true;
    }

    public static List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
