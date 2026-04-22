package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Stats {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args){
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /blockparty stats <player_name>");
            BlockParty.getInstance().debugLog("Stats command executed by " + sender.getName() + " with insufficient arguments");
            return false;
        }

        String playerName = args[1];

        // Get player stats from database
        Map<String, Object> stats = StatsManager.getPlayerStats(playerName);

        if (stats.containsKey("error")) {
            sender.sendMessage(ChatColor.RED + "Error retrieving stats.");
            BlockParty.getInstance().errorLog("Error retrieving stats for " + playerName);
            return false;
        }

        if (stats.containsKey("not_found")) {
            sender.sendMessage(ChatColor.YELLOW + "No stats found for player: " + playerName);
            BlockParty.getInstance().debugLog("Stats command: No stats found for " + playerName);
            return true;
        }

        // Display player stats
        sender.sendMessage(ChatColor.GOLD + "====== Stats for " + playerName + " ======");
        sender.sendMessage(ChatColor.YELLOW + "Games Played: " + ChatColor.RESET + stats.get("games_played"));
        sender.sendMessage(ChatColor.YELLOW + "Games Won: " + ChatColor.RESET + stats.get("games_won"));
        sender.sendMessage(ChatColor.YELLOW + "Win Rate: " + ChatColor.RESET + stats.get("win_rate") + "%");
        sender.sendMessage(ChatColor.YELLOW + "Best Round: " + ChatColor.RESET + stats.get("best_round"));
        sender.sendMessage(ChatColor.YELLOW + "Total Eliminations: " + ChatColor.RESET + stats.get("total_eliminations"));

        long lastPlayed = (long) stats.get("last_played");
        if (lastPlayed > 0) {
            long timeAgo = System.currentTimeMillis() - lastPlayed;
            long seconds = timeAgo / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;

            String timeString;
            if (days > 0) {
                timeString = days + " day" + (days > 1 ? "s" : "") + " ago";
            } else if (hours > 0) {
                timeString = hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (minutes > 0) {
                timeString = minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
            } else {
                timeString = "just now";
            }
            sender.sendMessage(ChatColor.YELLOW + "Last Played: " + ChatColor.RESET + timeString);
        }

        sender.sendMessage(ChatColor.GOLD + "============================");
        BlockParty.getInstance().debugLog("Stats command executed for player: " + playerName + " by " + sender.getName());
        return true;
    }

    public static List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerNames.add(player.getName());
            }
            return StringUtil.copyPartialMatches(args[1], playerNames, new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
