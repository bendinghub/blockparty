package me.unprankable.blockparty.commands;

import me.unprankable.blockparty.BlockParty;
import me.unprankable.blockparty.managers.ConfigManager;
import me.unprankable.blockparty.managers.GameManager;
import me.unprankable.blockparty.managers.GameSession;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class Reload {
    public static boolean execute(CommandSender sender, Command command, String label, String[] args) {
        int stoppedGames = GameManager.stopAllGameSessions();
        GameManager.clearAllData();
        GameSession.clearPendingHotbarRestores();
        ConfigManager.reload();

        sender.sendMessage(ChatColor.GREEN + "BlockParty config reloaded.");
        sender.sendMessage(ChatColor.YELLOW + "Stopped active games: " + ChatColor.RESET + stoppedGames);
        BlockParty.getInstance().debugLog("BlockParty reloaded by " + sender.getName() + ", stopped games: " + stoppedGames);
        return true;
    }

    public static List<String> tabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}

