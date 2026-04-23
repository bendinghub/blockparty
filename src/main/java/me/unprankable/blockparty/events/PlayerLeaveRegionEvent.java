package me.unprankable.blockparty.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Sent when a player leaves a block party region.
 */
public class PlayerLeaveRegionEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final String region;
    private final String name;
    private final UUID playerId;
    private final RegionLeaveCause cause;
    private boolean cancel = false;

    public PlayerLeaveRegionEvent(String name, UUID playerId, String region, RegionLeaveCause cause) {
        this.region = region;
        this.name = name;
        this.playerId = playerId;
        this.cause = cause;
    }

    /**
     * Returns the name of the player who left the region
     * @return Name of the player who left the region
     */
    public String getPlayerName() {
        return name;
    }

    /**
     * Returns the UUID of the player who left the region
     * @return UUID of the player who left the region
     */
    public UUID getPlayerId() {
        return playerId;
    }
    /**
     * Returns the region the player left
     * @return Region the player left
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the cause for the player leaving the region
     * @return Cause for the player leaving the reason
     */
    public RegionLeaveCause getCause() {
        return cause;
    }

    /**
     * {@inheritDoc}
     * Can only be cancelled for events caused by {@link RegionLeaveCause#COMMAND COMMAND}, {@link RegionLeaveCause#ELIMINATION ELIMINATION}, and {@link RegionLeaveCause#PLUGIN PLUGIN}
     * @param cancel true if you wish to cancel this event
     * @throws IllegalStateException if the event cannot be cancelled
     */
    @Override
    public void setCancelled(boolean cancel) {
        if (cause != RegionLeaveCause.COMMAND && cause != RegionLeaveCause.PLUGIN) {
            throw new IllegalStateException("PlayerLeaveRegionEvent not caused by COMMAND, ELIMINATION, or PLUGIN cannot be cancelled.");
        }
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

    public enum RegionLeaveCause {
        /**
         * Player left using /bp leave
         */
        COMMAND,
        /**
         * Player left the region because they left the server
         */
        DISCONNECT,
        /**
         * Player left the region because the game ended or the region was otherwise cleared
         */
        GAME_END,
        /**
         * A plugin kicked the player from the region
         */
        PLUGIN
    }
}
