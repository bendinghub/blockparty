package me.unprankable.blockparty.hooks;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.world.World;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public final class WorldEditHook {
    private WorldEditHook (){
        //utility class - no instances
    }
    @Nullable
    public static Region getPlayerSelection(Player player){
        if (player == null) return null;
        Actor actor = BukkitAdapter.adapt(player);
        SessionManager manager = WorldEdit.getInstance().getSessionManager();
        LocalSession session = manager.get(actor);
        World selectionWorld = session.getSelectionWorld();
        if (selectionWorld == null){
            player.sendMessage(ChatColor.RED + "No selection world set");
            return null;
        }
        Region region;
        try {
            region = session.getSelection(selectionWorld);
            return region;
        } catch (IncompleteRegionException e){
            player.sendMessage(ChatColor.RED + "Incomplete selection! set both positions.");
            return null;
        }
    }
    @Nullable
    public static int[] getFirstPosition(Region region){
        if(region == null){
            return null;
        }
        BlockVector3 min = region.getMinimumPoint();
        return new int[] {min.x(), min.y(), min.z()};
    }
    @Nullable
    public static int[] getSecondPosition(Region region){
        if(region == null){
            return null;
        }
        BlockVector3 max = region.getMaximumPoint();
        return new int[] {max.x(), max.y(), max.z()};
    }
    @Nullable
    public static String getWorldName(Region region){
        if(region == null){
            return null;
        }
        World world = region.getWorld();
        if(world == null){
            return null;
        }
        return world.getName();
    }
}
