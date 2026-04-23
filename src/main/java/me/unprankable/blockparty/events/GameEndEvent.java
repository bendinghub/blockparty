package me.unprankable.blockparty.events;

import me.unprankable.blockparty.managers.GameSession;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Sent when a block party game ends
 */
public class GameEndEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private String winner;
    private boolean hasWinner;
    private final String regionName;
    private final GameSession gameSession;

    public GameEndEvent(String winner, boolean hasWinner, String regionName, GameSession gameSession) {
        this.winner = winner;
        this.hasWinner = hasWinner;
        this.regionName = regionName;
        this.gameSession = gameSession;
    }

    /**
     * Returns the winner of the game
     * @return Winner of the game. If there is no winner, returns "Nobody".
     */
    public String getWinner() {
        return winner;
    }
    /**
     * Returns true if the game has any winner
     * @return True if the game has any winner
     */
    public boolean hasWinner() {
        return hasWinner;
    }
    /**
     * Sets a new winner for the game
     * @param winner The new winner
     */
    public void setWinner(String winner) {
        this.hasWinner = true;
        this.winner = winner;
    }
    /**
     * Sets the game to have no winners
     */
    public void setNoWinner() {
        this.winner = "Nobody";
        this.hasWinner = false;
    }
    /**
     * Returns the name of the region whose game ended
     * @return Name of the region whose game ended
     */
    public String getRegion() {
        return regionName;
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
