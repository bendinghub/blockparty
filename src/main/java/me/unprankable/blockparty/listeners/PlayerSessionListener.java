package me.unprankable.blockparty.listeners;

import me.unprankable.blockparty.events.PlayerLeaveRegionEvent.RegionLeaveCause;
import me.unprankable.blockparty.managers.GameManager;
import me.unprankable.blockparty.managers.GameSession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerSessionListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        handleDisconnect(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        GameSession.restorePendingHotbar(event.getPlayer());
    }

    private void handleDisconnect(java.util.UUID playerId) {
        String regionName = GameManager.getPlayerRegion(playerId);
        if (regionName == null) {
            return;
        }

        GameManager.removePlayerFromRegion(playerId, regionName, RegionLeaveCause.DISCONNECT);
        GameSession session = GameManager.getGameSession(regionName);
        if (session != null) {
            session.handlePlayerCountChanged();
        }
    }
}
