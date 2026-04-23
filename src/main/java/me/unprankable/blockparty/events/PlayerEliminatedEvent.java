package me.unprankable.blockparty.events;

import me.unprankable.blockparty.managers.GameSession;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.UUID;

/**
 * Sent when a player is eliminated in a block party game
 */
public class PlayerEliminatedEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final String region;
    private final GameSession gameSession;
    private boolean cancel = false;

    public PlayerEliminatedEvent(Player who, String region, GameSession gameSession) {
        super(who);
        this.region = region;
        this.gameSession = gameSession;
    }

    /**
     * Returns the region of the game the player is playing
     * @return Region the game the player is playing
     */
    public String getRegion() {
        return region;
    }
    /**
     * Returns the game session
     * @return Game session
     */
    public GameSession getGameSession() {
        return gameSession;
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
