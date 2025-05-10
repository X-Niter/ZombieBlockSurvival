package com.seventodie.quests;

import org.bukkit.Location;

import com.seventodie.quests.QuestManager.QuestTargetType;

import java.util.UUID;

/**
 * Represents a quest in the SevenToDie plugin
 */
public class Quest {
    
    private final UUID id;
    private final String title;
    private final String description;
    private final QuestTargetType targetType;
    private final int targetAmount;
    private final Location location;
    private final UUID structureId;
    
    private int progress;
    private boolean completed;
    
    /**
     * Constructor for a Quest
     * 
     * @param id The quest ID
     * @param title The quest title
     * @param description The quest description
     * @param targetType The target type
     * @param targetAmount The target amount
     * @param location The quest location
     * @param structureId The associated structure ID, or null
     */
    public Quest(UUID id, String title, String description, QuestTargetType targetType, int targetAmount, Location location, UUID structureId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.targetType = targetType;
        this.targetAmount = targetAmount;
        this.location = location;
        this.structureId = structureId;
        this.progress = 0;
        this.completed = false;
    }
    
    /**
     * Get the quest ID
     * 
     * @return The ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Get the quest title
     * 
     * @return The title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Get the quest description
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the target type
     * 
     * @return The target type
     */
    public QuestTargetType getTargetType() {
        return targetType;
    }
    
    /**
     * Get the target amount
     * 
     * @return The target amount
     */
    public int getTargetAmount() {
        return targetAmount;
    }
    
    /**
     * Get the quest location
     * 
     * @return The location
     */
    public Location getLocation() {
        return location;
    }
    
    /**
     * Get the associated structure ID
     * 
     * @return The structure ID, or null
     */
    public UUID getStructureId() {
        return structureId;
    }
    
    /**
     * Get the quest progress
     * 
     * @return The progress
     */
    public int getProgress() {
        return progress;
    }
    
    /**
     * Get the quest progress for a player
     * 
     * @param playerId The player UUID
     * @return The progress
     */
    public int getProgress(UUID playerId) {
        // For now, we're not tracking per-player progress
        // so we just return the overall progress
        return progress;
    }
    
    /**
     * Check if a player has completed this quest
     * 
     * @param playerId The player ID
     * @return True if completed
     */
    public boolean isCompletedByPlayer(UUID playerId) {
        // For now, we're not tracking per-player completion
        // so we just return the overall completion status
        return completed;
    }
    
    /**
     * Set the quest progress
     * 
     * @param progress The new progress
     */
    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(progress, targetAmount));
    }
    
    /**
     * Check if the quest is completed
     * 
     * @return True if completed
     */
    public boolean isCompleted() {
        return completed;
    }
    
    /**
     * Set whether the quest is completed
     * 
     * @param completed True if completed
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    /**
     * Calculate the completion percentage
     * 
     * @return The percentage (0-100)
     */
    public int getCompletionPercentage() {
        return (progress * 100) / Math.max(1, targetAmount);
    }
    
    @Override
    public String toString() {
        return "Quest{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", targetType=" + targetType +
                ", progress=" + progress + "/" + targetAmount +
                ", completed=" + completed +
                '}';
    }
}