package me.unprankable.blockparty.events;

import me.unprankable.blockparty.managers.GameSession;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Sent when a new round starts in a block party game. Avoid calling methods like {@link GameSession#stop()} directly in a RoundStartEvent.
 */
public class RoundStartEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final int roundNumber;
    private final String regionName;
    private final GameSession gameSession;
    private Material block;

    public RoundStartEvent(int roundNumber, String regionName, Material block, GameSession gameSession) {
        this.roundNumber = roundNumber;
        this.regionName = regionName;
        this.block = block;
        this.gameSession = gameSession;
    }

    /**
     * Returns the round number of the round that was started
     * @return Round number of the round that was started
     */
    public int getRoundNumber() {
        return roundNumber;
    }
    /**
     * Returns the name of the region that the game is taking place in
     * @return Name of the region that the game is taking place in
     */
    public String getRegion() {
        return regionName;
    }
    /**
     * Returns the chosen block that the players have to stand on
     * @return Chosen block
     */
    public Material getChosenBlock() {
        return block;
    }
    /**
     * Sets the chosen block that the players will have to stand on
     * @param newBlock The new chosen block
     */
    public void setChosenBlock(Material newBlock) {
        if (newBlock == null) {
            throw new IllegalStateException("The chosen block cannot be null");
        }
        block = newBlock;
    }
    /**
     * Returns the game session
     * @return Game session
     */
    public GameSession getGameSession() {
        return gameSession;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
