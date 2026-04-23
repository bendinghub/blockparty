package me.unprankable.blockparty;

import me.unprankable.blockparty.commands.Create;
import me.unprankable.blockparty.commands.Delete;
import me.unprankable.blockparty.commands.Edit;
import me.unprankable.blockparty.commands.Help;
import me.unprankable.blockparty.commands.Info;
import me.unprankable.blockparty.commands.Join;
import me.unprankable.blockparty.commands.Leave;
import me.unprankable.blockparty.commands.Reload;
import me.unprankable.blockparty.commands.Start;
import me.unprankable.blockparty.commands.Stats;
import me.unprankable.blockparty.commands.Stop;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class commandExecutor implements CommandExecutor, TabCompleter {
    private static final java.util.List<String> SUBCOMMANDS = Arrays.asList(
            "create", "delete", "edit", "info", "join", "leave", "list", "reload", "start", "stop", "stats", "help"
    );

    public commandExecutor(BlockParty plugin){
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0){
            return false;
        }
        if (SUBCOMMANDS.contains(args[0].toLowerCase()) && !sender.hasPermission("blockparty." + args[0].toLowerCase())) {
            sender.sendMessage(ChatColor.RED + "No permission");
            return true;
        }
        switch(args[0].toLowerCase()){
            case "create" -> Create.execute(sender, command, label, args);
            case "delete" -> Delete.execute(sender, command, label, args);
            case "edit" -> Edit.execute(sender, command, label, args);
            case "info" -> Info.execute(sender, command, label, args);
            case "join" -> Join.execute(sender, command, label, args);
            case "leave" -> Leave.execute(sender, command, label, args);
            case "list" -> me.unprankable.blockparty.commands.List.execute(sender, command, label, args);
            case "reload" -> Reload.execute(sender, command, label, args);
            case "start" -> Start.execute(sender, command, label, args);
            case "stop" -> Stop.execute(sender, command, label, args);
            case "stats" -> Stats.execute(sender, command, label, args);
            case "help" -> Help.execute(sender, command, label, args);
            default -> {
                if (!sender.hasPermission("blockparty.help")) {
                    sender.sendMessage(ChatColor.RED + "Unknown command, no permission to see help");
                    return true;
                }
                Help.execute(sender, command, label, args);
            }
        }
        return true;
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            String input = args.length == 0 ? "" : args[0];
            return StringUtil.copyPartialMatches(input, SUBCOMMANDS.stream().filter(cmd -> sender.hasPermission("blockparty." + cmd)).toList(), new ArrayList<>());
        }

        String subcommand = args[0].toLowerCase();
        if (sender.hasPermission("blockparty." + subcommand)) {
            if (subcommand.equals("create")) return Create.tabComplete(sender, command, alias, args);
            if (subcommand.equals("delete")) return Delete.tabComplete(sender, command, alias, args);
            if (subcommand.equals("edit")) return Edit.tabComplete(sender, command, alias, args);
            if (subcommand.equals("info")) return Info.tabComplete(sender, command, alias, args);
            if (subcommand.equals("join")) return Join.tabComplete(sender, command, alias, args);
            if (subcommand.equals("leave")) return Leave.tabComplete(sender, command, alias, args);
            if (subcommand.equals("list")) return me.unprankable.blockparty.commands.List.tabComplete(sender, command, alias, args);
            if (subcommand.equals("reload")) return Reload.tabComplete(sender, command, alias, args);
            if (subcommand.equals("start")) return Start.tabComplete(sender, command, alias, args);
            if (subcommand.equals("stop")) return Stop.tabComplete(sender, command, alias, args);
            if (subcommand.equals("stats")) return Stats.tabComplete(sender, command, alias, args);
            if (subcommand.equals("help")) return Help.tabComplete(sender, command, alias, args);
        }
        return Collections.emptyList();
    }
}
