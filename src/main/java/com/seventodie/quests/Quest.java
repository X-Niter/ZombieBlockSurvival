package com.seventodie.quests;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;

import com.seventodie.quests.QuestManager.QuestTargetType;

/**
 * Represents a quest in the game.
 */
public class Quest {
    
    private final UUID id;
    private final String title;
    private final String description;
    private final QuestTargetType targetType;
    private final int targetAmount;
    private final Location location;
    private final UUID structureId;
    
    private boolean completed;
    private Object markerRef;
    private Object secondaryMarkerRef;
    
    // Track player progress
    private final Map<UUID, Integer> playerProgress = new HashMap<>();
    
    /**
     * Creates a new quest
     * 
     * @param id The quest ID
     * @param title The quest title
     * @param description The quest description
     * @param targetType The quest target type
     * @param targetAmount The target amount to complete
     * @param location The quest location
     * @param structureId The structure ID this quest is tied to, or null
     */
    public Quest(UUID id, String title, String description, QuestTargetType targetType, 
                int targetAmount, Location location, UUID structureId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetType = targetType;
        this.targetAmount = targetAmount;
        this.location = location;
        this.structureId = structureId;
        this.completed = false;
    }
    
    /**
     * Get the quest ID
     * 
     * @return The quest ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Get the quest title
     * 
     * @return The quest title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Get the quest description
     * 
     * @return The quest description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the quest target type
     * 
     * @return The quest target type
     */
    public QuestTargetType getTargetType() {
        return targetType;
    }
    
    /**
     * Get the target amount required to complete the quest
     * 
     * @return The target amount
     */
    public int getTargetAmount() {
        return targetAmount;
    }
    
    /**
     * Get the quest location
     * 
     * @return The quest location
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Get the structure ID this quest is tied to
     * 
     * @return The structure ID, or null if not tied to a structure
     */
    public UUID getStructureId() {
        return structureId;
    }
    
    /**
     * Check if the quest has been completed
     * 
     * @return True if the quest is completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Set whether the quest is completed
     * 
     * @param completed True if the quest is completed
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Get the marker reference (hologram or armor stand)
     * 
     * @return The marker reference
     */
    public Object getMarkerRef() {
        return markerRef;
    }
    
    /**
     * Set the marker reference
     * 
     * @param markerRef The marker reference
     */
    public void setMarkerRef(Object markerRef) {
        this.markerRef = markerRef;
    }
    
    /**
     * Get the secondary marker reference (for native markers)
     * 
     * @return The secondary marker reference
     */
    public Object getSecondaryMarkerRef() {
        return secondaryMarkerRef;
    }
    
    /**
     * Set the secondary marker reference
     * 
     * @param secondaryMarkerRef The secondary marker reference
     */
    public void setSecondaryMarkerRef(Object secondaryMarkerRef) {
        this.secondaryMarkerRef = secondaryMarkerRef;
    }
    
    /**
     * Get a player's progress on this quest
     * 
     * @param playerId The player UUID
     * @return The player's progress (0 if not started)
     */
    public int getProgress(UUID playerId) {
        return playerProgress.getOrDefault(playerId, 0);
    }
    
    /**
     * Set a player's progress on this quest
     * 
     * @param playerId The player UUID
     * @param progress The progress value
     */
    public void setProgress(UUID playerId, int progress) {
        playerProgress.put(playerId, progress);
    }
    
    /**
     * Check if a player has completed this quest
     * 
     * @param playerId The player UUID
     * @return True if the player has completed this quest
     */
    public boolean isCompletedByPlayer(UUID playerId) {
        return getProgress(playerId) >= targetAmount;
    }
    
    /**
     * Get the progress percentage for a player
     * 
     * @param playerId The player UUID
     * @return The progress percentage (0-100)
     */
    public int getProgressPercentage(UUID playerId) {
        int progress = getProgress(playerId);
        return (int) ((progress / (double) targetAmount) * 100);
    }
    
    /**
     * Reset the progress for all players
     */
    public void resetProgress() {
        playerProgress.clear();
    }
    
    /**
     * Get a string representation of this quest
     */
    @Override
    public String toString() {
        return "Quest[id=" + id + ", title=" + title + ", type=" + targetType + 
               ", target=" + targetAmount + ", completed=" + completed + "]";
    }
}
