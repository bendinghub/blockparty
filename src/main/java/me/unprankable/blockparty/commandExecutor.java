package me.unprankable.blockparty;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.unprankable.blockparty.commands.*;

public class commandExecutor implements CommandExecutor {
    private final BlockParty plugin;
    public commandExecutor(BlockParty plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0){
            return false;
        }
        switch(args[0].toLowerCase()){
            case "create" -> Create.execute(sender, command, label, args);
            case "delete" -> Delete.execute(sender, command, label, args);
            case "edit" -> Edit.execute(sender, command, label, args);
            case "info" -> Info.execute(sender, command, label, args);
            case "join" -> Join.execute(sender, command, label, args);
            case "leave" -> Leave.execute(sender, command, label, args);
            case "list" -> List.execute(sender, command, label, args);
            case "start" -> Start.execute(sender, command, label, args);
            case "stop" -> Stop.execute(sender, command, label, args);
            case "stats" -> Stats.execute(sender, command, label, args);
            case "help" -> Help.execute(sender, command, label, args);
            default -> Help.execute(sender, command, label, args);
        }
        return true;
    }
}
