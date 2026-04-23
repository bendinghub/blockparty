package me.unprankable.blockparty.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Sent when a player joins a block party region.
 */
public class PlayerJoinRegionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final String region;
    private final String name;
    private final UUID playerId;
    private boolean cancel = false;

    public PlayerJoinRegionEvent(String name, UUID playerId, String region) {
        this.region = region;
        this.name = name;
        this.playerId = playerId;
    }

    /**
     * Returns the name of the player who joined the region
     * @return Name of the player who joined the region
     */
    public String getPlayerName() {
        return name;
    }

    /**
     * Returns the UUID of the player who joined the region
     * @return UUID of the player who joined the region
     */
    public UUID getPlayerId() {
        return playerId;
    }
    /**
     * Returns the region the player joined
     * @return Region the player joined
     */
    public String getRegion() {
        return region;
    }
    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
